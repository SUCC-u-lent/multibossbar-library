package com.ubunifu.multibossbar.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ubunifu.multibossbar.common.boss_bar.RowType;
import com.ubunifu.multibossbar.server.bossbar.ServerBarSection;
import com.ubunifu.multibossbar.server.bossbar.ServerMultibarContainer;
import net.minecraft.command.*;
import net.minecraft.command.argument.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class MultibossBarCommand {

    private static final Map<Identifier, ServerMultibarContainer> CONTAINERS =
            new HashMap<>();

    private static final Map<Identifier, Map<Identifier, ServerBarSection>> SECTIONS =
            new HashMap<>();

    private static final SimpleCommandExceptionType CONTAINER_NOT_FOUND =
            new SimpleCommandExceptionType(Text.literal("Multibar container not found."));

    private static final SimpleCommandExceptionType SECTION_NOT_FOUND =
            new SimpleCommandExceptionType(Text.literal("Bar section not found."));

    private static final SimpleCommandExceptionType ALREADY_EXISTS =
            new SimpleCommandExceptionType(Text.literal("That ID already exists."));

    private static final SimpleCommandExceptionType INVALID_VALUE =
            new SimpleCommandExceptionType(Text.literal("Invalid value."));
	public static final SuggestionProvider<ServerCommandSource> CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER = (context, builder) -> CommandSource.suggestIdentifiers(CONTAINERS.keySet(), builder);
	public static final SuggestionProvider<ServerCommandSource> SECTION_IDENTIFIER_SUGGESTION_PROVIDER = (context, builder) -> CommandSource.suggestIdentifiers(SECTIONS.keySet(), builder);

    private MultibossBarCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandBuildContext ctx) {
		if (!QuiltLoader.isDevelopmentEnvironment()) return;
        dispatcher.register(literal("multibossbar")
                .requires(source -> source.hasPermission(2))

                // /multibossbar create <id> <name>
                .then(literal("create")
                        .then(argument("id", IdentifierArgumentType.identifier())
                                .then(argument("name", TextArgumentType.text(ctx))
                                        .executes(context -> {
                                            Identifier id =
                                                    IdentifierArgumentType.getIdentifier(context, "id");

                                            if (CONTAINERS.containsKey(id)) {
                                                throw ALREADY_EXISTS.create();
                                            }

                                            ServerMultibarContainer container =
                                                    new ServerMultibarContainer();

                                            container.setDisplayName(
												TextArgumentType.getTextArgument(context, "name"));

                                            CONTAINERS.put(id, container);
                                            SECTIONS.put(id, new HashMap<>());

                                            context.getSource().sendFeedback(
                                                    () -> Text.literal("Created multibar " + id),
                                                    true
                                            );
                                            return 1;
                                        }))))

                // /multibossbar remove <id>
                .then(literal("remove")
                        .then(argument("id", IdentifierArgumentType.identifier())
							.suggests(CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    Identifier id =
                                            IdentifierArgumentType.getIdentifier(context, "id");

                                    ServerMultibarContainer container = getContainer(id);

                                    // Tell attached clients to remove this container.
                                    container.clearPlayers();

                                    CONTAINERS.remove(id);
                                    SECTIONS.remove(id);

                                    context.getSource().sendFeedback(
                                            () -> Text.literal("Removed multibar " + id),
                                            true
                                    );
                                    return 1;
                                })))

                // /multibossbar list
                .then(literal("list")
                        .executes(context -> {
                            if (CONTAINERS.isEmpty()) {
                                context.getSource().sendFeedback(
                                        () -> Text.literal("No multibars have been created."),
                                        false
                                );
                                return 0;
                            }

                            CONTAINERS.forEach((id, container) ->
                                    context.getSource().sendFeedback(
                                            () -> Text.literal(
                                                    id + " - " +
                                                    container.getDisplayName().getString() +
                                                    " (" + container.getBars().size() +
                                                    " sections)"
                                            ),
                                            false
                                    )
                            );

                            return CONTAINERS.size();
                        }))

                // /multibossbar set <id> ...
                .then(literal("set")
                        .then(argument("id", IdentifierArgumentType.identifier())
							.suggests(CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER)
                                .then(literal("name")
                                        .then(argument("name", TextArgumentType.text(ctx))
                                                .executes(context -> {
                                                    ServerMultibarContainer container =
                                                            getContainer(context);

                                                    container.setDisplayName(
                                                            TextArgumentType.getTextArgument(
                                                                    context, "name"));

                                                    return 1;
                                                })))

                                .then(literal("visible")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    getContainer(context).setVisible(
                                                            BoolArgumentType.getBool(context, "value"));
                                                    return 1;
                                                })))

                                .then(literal("darken_sky")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    getContainer(context).setDarkenSky(
                                                            BoolArgumentType.getBool(context, "value"));
                                                    return 1;
                                                })))

                                .then(literal("dragon_music")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    getContainer(context).setDragonMusic(
                                                            BoolArgumentType.getBool(context, "value"));
                                                    return 1;
                                                })))

                                .then(literal("thicken_fog")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    getContainer(context).setThickenFog(
                                                            BoolArgumentType.getBool(context, "value"));
                                                    return 1;
                                                })))))

                // /multibossbar players add|remove <id> <player>
                .then(literal("players")
                        .then(literal("add")
                                .then(argument("id", IdentifierArgumentType.identifier())
									.suggests(CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER)
                                        .then(argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerMultibarContainer container =
                                                            getContainer(context);

                                                    ServerPlayerEntity player =
                                                            EntityArgumentType.getPlayer(context, "player");

                                                    if (!container.addPlayer(player)) {
                                                        throw INVALID_VALUE.create();
                                                    }

                                                    context.getSource().sendFeedback(
                                                            () -> Text.literal(
                                                                    "Added " + player.getName().getString() +
                                                                    " to " +
                                                                    IdentifierArgumentType.getIdentifier(
                                                                            context, "id")),
                                                            true
                                                    );
                                                    return 1;
                                                }))))

                        .then(literal("remove")
                                .then(argument("id", IdentifierArgumentType.identifier())
									.suggests(CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER)
                                        .then(argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerMultibarContainer container =
                                                            getContainer(context);

                                                    ServerPlayerEntity player =
                                                            EntityArgumentType.getPlayer(context, "player");

                                                    if (!container.removePlayer(player)) {
                                                        throw INVALID_VALUE.create();
                                                    }

                                                    return 1;
                                                })))))

                // /multibossbar section add|remove <container> <section>
                .then(literal("section")
                        .then(literal("add")
                                .then(argument("id", IdentifierArgumentType.identifier())
									.suggests(CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER)
                                        .then(argument("section_id",
                                                IdentifierArgumentType.identifier())
											.suggests(SECTION_IDENTIFIER_SUGGESTION_PROVIDER)
                                                .then(argument("name", TextArgumentType.text(ctx))
                                                        .executes(context -> {
                                                            Identifier containerId =
                                                                    IdentifierArgumentType.getIdentifier(
                                                                            context, "id");

                                                            Identifier sectionId =
                                                                    IdentifierArgumentType.getIdentifier(
                                                                            context, "section_id");

                                                            ServerMultibarContainer container =
                                                                    getContainer(containerId);

                                                            Map<Identifier, ServerBarSection> sections =
                                                                    SECTIONS.get(containerId);

                                                            if (sections.containsKey(sectionId)) {
                                                                throw ALREADY_EXISTS.create();
                                                            }

                                                            ServerBarSection section =
                                                                    new ServerBarSection(
                                                                            container,
                                                                            UUID.randomUUID(),
                                                                                    TextArgumentType.getTextArgument(
                                                                                            context, "name"),
                                                                            1.0,
                                                                            BossBar.Color.PURPLE,
                                                                            BossBar.Style.PROGRESS,
                                                                            RowType.WIDE,
                                                                            9.0f
                                                                    );

                                                            /*
                                                             * ServerBarSection sends CREATE from its
                                                             * constructor. Add directly to the backing
                                                             * list to avoid a second CREATE packet from
                                                             * ServerMultibarContainer.addSection().
                                                             */
                                                            container.getBars().add(section);
                                                            sections.put(sectionId, section);

                                                            context.getSource().sendFeedback(
                                                                    () -> Text.literal(
                                                                            "Created section " + sectionId),
                                                                    true
                                                            );
                                                            return 1;
                                                        })))))

                        .then(literal("remove")
                                .then(argument("id", IdentifierArgumentType.identifier())
									.suggests(CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER)
                                        .then(argument("section_id",
                                                IdentifierArgumentType.identifier())
											.suggests(SECTION_IDENTIFIER_SUGGESTION_PROVIDER)
                                                .executes(context -> {
                                                    Identifier containerId =
                                                            IdentifierArgumentType.getIdentifier(
                                                                    context, "id");

                                                    Identifier sectionId =
                                                            IdentifierArgumentType.getIdentifier(
                                                                    context, "section_id");

                                                    ServerMultibarContainer container =
                                                            getContainer(containerId);

                                                    ServerBarSection section =
                                                            getSection(containerId, sectionId);

                                                    container.removeSection(section);
                                                    SECTIONS.get(containerId).remove(sectionId);

                                                    return 1;
                                                })))

                        // /multibossbar section set <id> <section_id> ...
                        .then(literal("set")
                                .then(argument("id", IdentifierArgumentType.identifier())
									.suggests(CONTAINER_IDENTIFIER_SUGGESTION_PROVIDER)
                                        .then(argument("section_id",
                                                IdentifierArgumentType.identifier())
											.suggests(SECTION_IDENTIFIER_SUGGESTION_PROVIDER)

                                                .then(literal("name")
                                                        .then(argument("value",
                                                                TextArgumentType.text(ctx))
                                                                .executes(context -> {
                                                                    getSection(context).setName(
																				TextArgumentType.getTextArgument(
                                                                                            context, "value"));
                                                                    return 1;
                                                                })))

                                                .then(literal("value")
                                                        .then(argument("value",
                                                                DoubleArgumentType.doubleArg(0.0, 1.0))
                                                                .executes(context -> {
                                                                    getSection(context).setPercentage(
                                                                            DoubleArgumentType.getDouble(
                                                                                    context, "value"));
                                                                    return 1;
                                                                })))

                                                .then(literal("color")
                                                        .then(argument("value",
                                                                StringArgumentType.word())
                                                                .executes(context -> {
                                                                    getSection(context).setColor(
                                                                            parseEnum(
                                                                                    BossBar.Color.class,
                                                                                    StringArgumentType.getString(
                                                                                            context, "value")));
                                                                    return 1;
                                                                })))

                                                .then(literal("style")
                                                        .then(argument("value",
                                                                StringArgumentType.word())
                                                                .executes(context -> {
                                                                    getSection(context).setStyle(
                                                                            parseEnum(
                                                                                    BossBar.Style.class,
                                                                                    StringArgumentType.getString(
                                                                                            context, "value")));
                                                                    return 1;
                                                                })))

                                                .then(literal("type")
                                                        .then(argument("value",
                                                                StringArgumentType.word())
                                                                .executes(context -> {
                                                                    getSection(context).setType(
                                                                            parseEnum(
                                                                                    RowType.class,
                                                                                    StringArgumentType.getString(
                                                                                            context, "value")));
                                                                    return 1;
                                                                })))

                                                .then(literal("scale")
                                                        .then(argument("value",
                                                                FloatArgumentType.floatArg(5.0f, 20.0f))
                                                                .executes(context -> {
                                                                    getSection(context).setVerticalScale(
                                                                            FloatArgumentType.getFloat(
                                                                                    context, "value"));
                                                                    return 1;
                                                                })))

                                                .then(literal("visible")
                                                        .then(argument("value",
                                                                BoolArgumentType.bool())
                                                                .executes(context -> {
                                                                    getSection(context).setVisible(
                                                                            BoolArgumentType.getBool(
                                                                                    context, "value"));
                                                                    return 1;
                                                                })
														)
												)
										)
								)
						))
				)
		);

        // These IDs are runtime-only; discard stale references when the server stops.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            CONTAINERS.clear();
            SECTIONS.clear();
        });
    }

    private static ServerMultibarContainer getContainer(Identifier id)
            throws CommandSyntaxException {
        ServerMultibarContainer container = CONTAINERS.get(id);

        if (container == null) {
            throw CONTAINER_NOT_FOUND.create();
        }

        return container;
    }

    private static ServerMultibarContainer getContainer(
            com.mojang.brigadier.context.CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        return getContainer(IdentifierArgumentType.getIdentifier(context, "id"));
    }

    private static ServerBarSection getSection(
            com.mojang.brigadier.context.CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        Identifier containerId =
                IdentifierArgumentType.getIdentifier(context, "id");

        Identifier sectionId =
                IdentifierArgumentType.getIdentifier(context, "section_id");

        return getSection(containerId, sectionId);
    }

    private static ServerBarSection getSection(
            Identifier containerId, Identifier sectionId)
            throws CommandSyntaxException {
        getContainer(containerId);

        Map<Identifier, ServerBarSection> sections = SECTIONS.get(containerId);
        ServerBarSection section = sections == null ? null : sections.get(sectionId);

        if (section == null) {
            throw SECTION_NOT_FOUND.create();
        }

        return section;
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value)
            throws CommandSyntaxException {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new SimpleCommandExceptionType(
                    Text.literal("Invalid " + enumClass.getSimpleName() + ": " + value)
            ).create();
        }
    }
}
