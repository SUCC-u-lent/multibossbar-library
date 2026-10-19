package com.ubunifu.multibossbar.common.command;

import com.mojang.brigadier.*;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import com.mojang.brigadier.suggestion.*;
import com.ubunifu.multibossbar.common.MultibossbarMod;
import com.ubunifu.multibossbar.common.boss_bar.FillType;
import com.ubunifu.multibossbar.common.boss_bar.SectionType;
import com.ubunifu.multibossbar.common.boss_bar.container.ServerMultiBossBar;
import com.ubunifu.multibossbar.common.boss_bar.sections.ServerBossBarSection;
import com.ubunifu.multibossbar.common.boss_bar.sections.framework.BossBarSection;
import com.ubunifu.multibossbar.common.networking.MultibossBarS2CPacket;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.registry.HolderLookup;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class MultibossBarCommand {
	private static final Map<String, ServerMultiBossBar> BARS = new HashMap<>();

	private MultibossBarCommand() {}

	public static void register(
		CommandDispatcher<ServerCommandSource> dispatcher,
		HolderLookup.Provider registries
	) {
		LiteralArgumentBuilder<ServerCommandSource> root =
			CommandManager.literal("multibossbar")
				.requires(source -> source.hasPermission(2));

		// /multibossbar create <id> <display-name-json...>
		root.then(CommandManager.literal("create")
			.then(CommandManager.argument("id", StringArgumentType.word())
				.executes((ctx)->create(ctx.getSource(),StringArgumentType.getString(ctx, "id"),""))
				.then(CommandManager.argument(
							"displayName",
							StringArgumentType.greedyString()
						)
						.executes(ctx -> create(
							ctx.getSource(),
							StringArgumentType.getString(ctx, "id"),
							StringArgumentType.getString(ctx, "displayName")
						))
				)
			)
		);

		// /multibossbar remove <bar>
		root.then(CommandManager.literal("remove")
			.then(existingBar("bar")
				.executes(ctx -> remove(
					ctx.getSource(),
					StringArgumentType.getString(ctx, "bar")
				))
			)
		);

		// /multibossbar list
		root.then(CommandManager.literal("list")
			.executes(ctx -> list(ctx.getSource()))
		);

		// /multibossbar show|hide <bar>
		root.then(visibilityCommand("show", true));
		root.then(visibilityCommand("hide", false));

		// /multibossbar players set|add|remove <bar> <players>
		LiteralArgumentBuilder<ServerCommandSource> players =
			CommandManager.literal("players");

		players.then(playerCommand("set"));
		players.then(playerCommand("add"));
		players.then(playerCommand("remove"));

		root.then(players);

		// Section management.
		LiteralArgumentBuilder<ServerCommandSource> section =
			CommandManager.literal("section");

		section.then(CommandManager.literal("add")
			.then(existingBar("bar")
				.then(CommandManager.argument("height", IntegerArgumentType.integer(5, 20))
					.then(CommandManager.argument("type",  StringArgumentType.word())
						.suggests((context, builder) ->
							CommandSource.suggestMatching(
								Arrays.stream(SectionType.values())
									.map(color -> color.name().toLowerCase(Locale.ROOT)),
								builder
							)
						)
						.then(CommandManager.argument("color",  StringArgumentType.word())
							.suggests((context, builder) ->
								CommandSource.suggestMatching(
									Arrays.stream(BossBar.Color.values())
										.map(color -> color.name().toLowerCase(Locale.ROOT)),
									builder
								)
							)
							.then(CommandManager.argument("style",  StringArgumentType.word())
								.suggests((context, builder) ->
									CommandSource.suggestMatching(
										Arrays.stream(BossBar.Style.values())
											.map(color -> color.name().toLowerCase(Locale.ROOT)),
										builder
									)
								)
								.executes((ctx)->addSection(
									ctx.getSource(),
									StringArgumentType.getString(ctx, "bar"),
									IntegerArgumentType.getInteger(ctx, "height"),
									parseEnum(SectionType.class, StringArgumentType.getString(ctx,"type")),
									parseEnum(BossBar.Color.class, StringArgumentType.getString(ctx,"color")),
									parseEnum(BossBar.Style.class, StringArgumentType.getString(ctx,"style")),
									""
								))
								.then(CommandManager.argument(
											"name",
											StringArgumentType.greedyString()
										)
										.executes(ctx -> addSection(
											ctx.getSource(),
											StringArgumentType.getString(ctx, "bar"),
											IntegerArgumentType.getInteger(ctx, "height"),
											parseEnum(SectionType.class, StringArgumentType.getString(ctx,"type")),
											parseEnum(BossBar.Color.class, StringArgumentType.getString(ctx,"color")),
											parseEnum(BossBar.Style.class, StringArgumentType.getString(ctx,"style")),
											StringArgumentType.getString(ctx, "name")
										))
								)
							)
						)
					)
				)
			)
		);

		section.then(CommandManager.literal("remove")
			.then(existingBar("bar")
				.then(sectionIndex()
					.executes(ctx -> removeSection(
						ctx.getSource(),
						StringArgumentType.getString(ctx, "bar"),
						IntegerArgumentType.getInteger(ctx, "index")
					))
				)
			)
		);

		section.then(sectionProperty("name", "name",
			StringArgumentType.greedyString(),
			(source, bar, index, value) -> {
				Text name = parseTextJson(source, value);
				if (name == null) return 0;

				ServerBossBarSection target = getSection(source, bar, index);
				if (target == null) return 0;

				target.setName(name);
				source.sendFeedback(
					() -> Text.literal("Updated the name of section " + index + " in '" + bar + "'."),
					false
				);
				return 1;
			}
		));

		section.then(CommandManager.literal("percent")
			.then(existingBar("bar")
				.then(sectionIndex()
					.then(CommandManager.argument("percent", FloatArgumentType.floatArg(0, 100))
						.executes(ctx -> {
							String bar = StringArgumentType.getString(ctx, "bar");
							int index = IntegerArgumentType.getInteger(ctx, "index");
							float percent = FloatArgumentType.getFloat(ctx, "percent");

							ServerBossBarSection target = getSection(
								ctx.getSource(), bar, index
							);
							if (target == null) return 0;

							target.setPercent(percent / 100.0F);
							ctx.getSource().sendFeedback(
								() -> Text.literal(
									"Set section " + index + " in '" + bar
										+ "' to " + percent + "%."
								),
								false
							);
							return 1;
						})
					)
				)
			)
		);

		section.then(CommandManager.literal("color")
			.then(existingBar("bar")
				.then(sectionIndex()
					.then(CommandManager.argument("color", StringArgumentType.word())
						.suggests((context, builder) ->
							CommandSource.suggestMatching(
								Arrays.stream(BossBar.Color.values())
									.map(color -> color.name().toLowerCase(Locale.ROOT)),
								builder
							)
						)
						.executes(ctx -> {
							String bar = StringArgumentType.getString(ctx, "bar");
							int index = IntegerArgumentType.getInteger(ctx, "index");
							BossBar.Color color = parseEnum(BossBar.Color.class, StringArgumentType.getString(ctx,"color"));

							if (color == null)
							{
								ctx.getSource().sendError(Text.literal("Invalid enum: "+StringArgumentType.getString(ctx,"color")));
								return 0;
							}

							ServerBossBarSection target = getSection(
								ctx.getSource(), bar, index
							);
							if (target == null) return 0;

							target.setColor(color);
							ctx.getSource().sendFeedback(
								() -> Text.literal(
									"Set section " + index + " in '" + bar
										+ "' to color " + color.name() + "."
								),
								false
							);
							return 1;
						})
					)
				)
			)
		);

		section.then(CommandManager.literal("style")
			.then(existingBar("bar")
				.then(sectionIndex()
					.then(CommandManager.argument("style", StringArgumentType.word())
						.suggests((context, builder) ->
							CommandSource.suggestMatching(
								Arrays.stream(BossBar.Color.values())
									.map(color -> color.name().toLowerCase(Locale.ROOT)),
								builder
							)
						)
						.executes(ctx -> {
							String bar = StringArgumentType.getString(ctx, "bar");
							int index = IntegerArgumentType.getInteger(ctx, "index");
							BossBar.Style style = parseEnum(BossBar.Style.class, StringArgumentType.getString(ctx,"style"));

							if (style == null)
							{
								ctx.getSource().sendError(Text.literal("Invalid enum: "+StringArgumentType.getString(ctx,"style")));
								return 0;
							}
							ServerBossBarSection target = getSection(
								ctx.getSource(), bar, index
							);
							if (target == null) return 0;

							target.setStyle(style);
							ctx.getSource().sendFeedback(
								() -> Text.literal(
									"Set section " + index + " in '" + bar
										+ "' to style " + style.name() + "."
								),
								false
							);
							return 1;
						})
					)
				)
			)
		);

		section.then(CommandManager.literal("type")
			.then(existingBar("bar")
				.then(sectionIndex()
					.then(CommandManager.argument("type", StringArgumentType.word())
						.suggests((context, builder) ->
							CommandSource.suggestMatching(
								Arrays.stream(SectionType.values())
									.map(color -> color.name().toLowerCase(Locale.ROOT)),
								builder
							)
						)
						.executes(ctx -> {
							String bar = StringArgumentType.getString(ctx, "bar");
							int index = IntegerArgumentType.getInteger(ctx, "index");
							SectionType type = parseEnum(SectionType.class, StringArgumentType.getString(ctx,"type"));

							if (type == null)
							{
								ctx.getSource().sendError(Text.literal("Invalid enum: "+StringArgumentType.getString(ctx,"type")));
								return 0;
							}
							ServerBossBarSection target = getSection(
								ctx.getSource(), bar, index
							);
							if (target == null) return 0;

							target.setSectionType(type);
							ctx.getSource().sendFeedback(
								() -> Text.literal(
									"Set section " + index + " in '" + bar
										+ "' to type " + type.name() + "."
								),
								false
							);
							return 1;
						})
					)
				)
			)
		);

		section.then(CommandManager.literal("height")
			.then(existingBar("bar")
				.then(sectionIndex()
					.then(CommandManager.argument("height", IntegerArgumentType.integer(1, 10))
						.executes(ctx -> {
							String bar = StringArgumentType.getString(ctx, "bar");
							int index = IntegerArgumentType.getInteger(ctx, "index");
							int height = IntegerArgumentType.getInteger(ctx, "height");

							ServerBossBarSection target = getSection(
								ctx.getSource(), bar, index
							);
							if (target == null) return 0;

							target.setScaleSupplier(height);
							ctx.getSource().sendFeedback(
								() -> Text.literal(
									"Set section " + index + " in '" + bar
										+ "' to height " + height + "."
								),
								false
							);
							return 1;
						})
					)
				)
			)
		);

		root.then(section);

		root.then(CommandManager.literal("help")
			.executes(ctx -> {
				ctx.getSource().sendFeedback(
					() -> Text.literal(
						"Use /multibossbar create, remove, list, show, hide, "
							+ "players, or section. Tab-complete arguments for options."
					),
					false
				);
				return 1;
			})
		);

		dispatcher.register(root);
	}

	private static <T extends Enum<T>> T parseEnum(Class<T> clazz, String name)
	{
		T val;
		try{
			val = Enum.valueOf(clazz,name);
		}catch (IllegalArgumentException ignored){
			try{
				val = Enum.valueOf(clazz,name.toUpperCase());
			}catch (IllegalArgumentException ignored2){
				try{
					val = Enum.valueOf(clazz,name.toLowerCase());
				}catch (IllegalArgumentException ignored3){
					val = null;
				}
			}
		}
		return val;
	}

	private static LiteralArgumentBuilder<ServerCommandSource> visibilityCommand(
		String literal,
		boolean visible
	) {
		return CommandManager.literal(literal)
			.then(existingBar("bar")
				.executes(ctx -> {
					String id = StringArgumentType.getString(ctx, "bar");
					ServerMultiBossBar bar = BARS.get(id);

					// The argument type checks existence during parsing.
					if (bar == null) return unknownBar(ctx.getSource(), id);

					bar.setVisible(visible);
					ctx.getSource().sendFeedback(
						() -> Text.literal(
							"Boss bar '" + id + "' is now "
								+ (visible ? "visible." : "hidden.")
						),
						false
					);
					return 1;
				})
			);
	}

	private static LiteralArgumentBuilder<ServerCommandSource> playerCommand(
		String operation
	) {
		return CommandManager.literal(operation)
			.then(existingBar("bar")
				.then(CommandManager.argument("players", EntityArgumentType.players())
					.executes(ctx -> {
						String id = StringArgumentType.getString(ctx, "bar");
						ServerMultiBossBar bar = BARS.get(id);
						if (bar == null) return unknownBar(ctx.getSource(), id);

						Collection<ServerPlayerEntity> selected =
							EntityArgumentType.getPlayers(ctx, "players");

						switch (operation) {
							case "set" ->
								bar.setPlayers(new HashSet<>(selected));
							case "add" ->
								selected.forEach(bar::addPlayer);
							case "remove" ->
								selected.forEach(bar::removePlayer);
							default -> throw new IllegalStateException(
								"Unhandled player operation: " + operation
							);
						}

						ctx.getSource().sendFeedback(
							() -> Text.literal(
								operation + " players " + selected.size()
									+ " player(s) " + operation + "ed for '" + id + "'."
							),
							false
						);
						return selected.size();
					})
				)
			);
	}

	private static int create(
		ServerCommandSource source,
		String id,
		String displayNameJson
	) {
		if (BARS.containsKey(id)) {
			source.sendError(Text.literal("A boss bar named '" + id + "' already exists."));
			return 0;
		}

		Text displayName = parseTextJson(source, displayNameJson);
		if (displayName == null) return 0;

		ServerMultiBossBar bar = new ServerMultiBossBar();

	    bar.setDisplayName(displayName);

		BARS.put(id, bar);

		// Create the client-side container before sending its bar data.
		try {
			ServerPlayerEntity player = source.getPlayer();
			MultibossBarS2CPacket.createBar(bar).send(player);
			bar.addPlayer(player);
		} catch (Exception ignored) {
			// Console and command-block sources do not have a player.
		}

		source.sendFeedback(
			() -> Text.literal("Created boss bar '" + id + "'."),
			false
		);
		return 1;
	}

	private static int remove(ServerCommandSource source, String id) {
		ServerMultiBossBar bar = BARS.remove(id);
		if (bar == null) return unknownBar(source, id);

		ServerMultiBossBar.removeBar(bar);

		source.sendFeedback(
			() -> Text.literal("Removed boss bar '" + id + "'."),
			false
		);
		return 1;
	}

	private static int list(ServerCommandSource source) {
		if (BARS.isEmpty()) {
			source.sendFeedback(
				() -> Text.literal("No boss bars are registered."),
				false
			);
			return 1;
		}

		BARS.forEach((id, bar) -> source.sendFeedback(
			() -> Text.literal(
				id + " — " + bar.count() + " section(s), "
					+ (bar.isVisible() ? "visible" : "hidden")
					+ ", " + bar.getPlayers().size() + " player(s)"
			),
			false
		));

		return BARS.size();
	}

	private static int addSection(
		ServerCommandSource source,
		String id,
		int height,
		SectionType type,
		BossBar.Color color,
		BossBar.Style style,
		String nameJson
	) {
		if (color == null || type == null || style == null)
		{
			source.sendError(Text.literal("Failed to add a section using an invalid or null enum."));
			return 0;
		}
		try{
			ServerMultiBossBar bar = BARS.get(id);
			if (bar == null) return unknownBar(source, id);

			Text name = parseTextJson(source, nameJson);
			if (name == null) return 0;

			int index = bar.count();
			ServerBossBarSection section = new ServerBossBarSection(
				index,
				name,
				color,
				style,
				type,
				height
			);

			section.setContainer(bar);
			bar.addBossBar(section);

			// This is a structural change, so resync the container.
			bar.resync();

			source.sendFeedback(
				() -> Text.literal("Added section " + index + " to '" + id + "'."),
				false
			);
			return 1;
		}catch (Exception e){
			MultibossbarMod.LOGGER.warn("An error occurred adding section",e);
			source.sendError(Text.literal("An error occurred adding section, check the console!"));
			return 0;
		}
	}
	@FunctionalInterface
	private interface SectionPropertySetter {
		int apply(
			ServerCommandSource source,
			String bar,
			int index,
			String value
		);
	}

	private static <T> LiteralArgumentBuilder<ServerCommandSource> sectionProperty(
		String literal,
		String argumentName,
		ArgumentType<T> argumentType,
		SectionPropertySetter setter
	) {
		return CommandManager.literal(literal)
			.then(existingBar("bar")
				.then(sectionIndex()
					.then(CommandManager.argument(argumentName, argumentType)
						.executes(ctx -> {
							try{
								//noinspection unchecked
								T value = (T) ctx.getArgument(argumentName, argumentType.getClass());
								return setter.apply(
									ctx.getSource(),
									StringArgumentType.getString(ctx, "bar"),
									IntegerArgumentType.getInteger(ctx, "index"),
									String.valueOf(value)
								);
							}catch (Exception ignored){}
							return 0;
						})
					)
				)
			);
	}
	private static int removeSection(
		ServerCommandSource source,
		String id,
		int index
	) {
		ServerMultiBossBar bar = BARS.get(id);
		if (bar == null) return unknownBar(source, id);
		if (!validIndex(source, bar, index)) return 0;

		bar.removeBossBar(index);
		bar.resync();

		source.sendFeedback(
			() -> Text.literal("Removed section " + index + " from '" + id + "'."),
			false
		);
		return 1;
	}

	private static ServerBossBarSection getSection(
		ServerCommandSource source,
		String id,
		int index
	) {
		ServerMultiBossBar bar = BARS.get(id);
		if (bar == null) {
			unknownBar(source, id);
			return null;
		}
		if (!validIndex(source, bar, index)) return null;

		BossBarSection section = bar.get(index);
		if (!(section instanceof ServerBossBarSection serverSection)) {
			source.sendError(Text.literal(
				"Section " + index + " is not a server-managed section."
			));
			return null;
		}

		return serverSection;
	}

	private static boolean validIndex(
		ServerCommandSource source,
		ServerMultiBossBar bar,
		int index
	) {
		if (index < 0 || index >= bar.count()) {
			source.sendError(Text.literal(
				"Invalid section index " + index
					+ ". Valid indices are 0 through " + (bar.count() - 1) + "."
			));
			return false;
		}
		return true;
	}

	private static int unknownBar(ServerCommandSource source, String id) {
		source.sendError(Text.literal("Unknown boss bar '" + id + "'."));
		return 0;
	}

	private static Text parseTextJson(
		ServerCommandSource source,
		String json
	) {
		if (json == null || json.isBlank() || json.equalsIgnoreCase("{}")) return Text.empty(); // Its fine to be blank.
		try {
			Text text = Text.SerializationUtil.fromJson(
				json,
				source.getRegistryManager()
			);

			if (text == null) {
				source.sendError(Text.literal(
					"Display-name JSON must represent a text component, not null."
				));
			}
			return text;
		} catch (Exception e) {
			source.sendError(Text.literal(
				"Invalid text JSON: " + e.getMessage()
			));
			return null;
		}
	}

	private static RequiredArgumentBuilder<ServerCommandSource, String> existingBar(
		String argumentName
	) {
		return CommandManager.argument(argumentName, StringArgumentType.word())
			.suggests((context, builder) -> {
				BARS.keySet().forEach(builder::suggest);
				return builder.buildFuture();
			});
	}

	private static RequiredArgumentBuilder<ServerCommandSource, Integer> sectionIndex() {
		return CommandManager.argument("index", IntegerArgumentType.integer(0))
			.suggests((context, builder) -> {
				String id = StringArgumentType.getString(context, "bar");
				ServerMultiBossBar bar = BARS.get(id);

				if (bar != null) {
					for (int i = 0; i < bar.count(); i++) {
						builder.suggest(i);
					}
				}

				return builder.buildFuture();
			});
	}
}
