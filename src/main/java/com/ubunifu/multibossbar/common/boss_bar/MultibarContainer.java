package com.ubunifu.multibossbar.common.boss_bar;

import com.ubunifu.multibossbar.server.bossbar.ServerBarSection;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.jetbrains.annotations.*;

import java.util.*;

public class MultibarContainer {
	public static final HashMap<UUID,MultibarContainer> MULTIBAR_CONTAINERS = new HashMap<>();

	@Deprecated
	public static @NotNull MultibarContainer getClientBar(UUID barUUID) {
		return Objects.requireNonNull(MULTIBAR_CONTAINERS.get(barUUID));
	}
	public static Optional<MultibarContainer> getOptionalClientBar(UUID barUUID) {
		return Optional.ofNullable(MULTIBAR_CONTAINERS.get(barUUID));
	}

	protected final UUID uuid;
	protected Text displayName;
	protected boolean darkenSky;
	protected boolean dragonMusic;
	protected boolean thickenFog;
	protected boolean visible;
	protected final List<BarSection> barSectionList = new ArrayList<>();

	/**
	 * Creates a section and adds it to the container.
	 * @param container The container to add it to.
	 * @param name The {@link Text} name of the bar. (Optional)
	 * @param percentage A 0.0 -> 1.0 number representing the fill, 0.0 being 0% and 1.0 being 100%
	 * @param color The color of the bar.
	 * @param style The style of the bar.
	 * @return A reference to the stored value, this means changing values in the returned object will reflect in the container.
	 */
	public static ServerBarSection createSection(@NotNull MultibarContainer container, @Nullable Text name, double percentage, BossBar.Color color, BossBar.Style style, RowType rowType, float verticalScale) {
		ServerBarSection section = new ServerBarSection(
			container,
			UUID.randomUUID(),
			name == null ? Text.empty() : name,
			Math.clamp(percentage,0.0,1.0),
			color,
			style,
			rowType,
			Math.clamp(verticalScale,name == null || name.getString().isBlank() ? 5 : 9,20)
		);
		container.addSection( section );
		return section;
	}
	public static ServerBarSection createSection(@NotNull MultibarContainer container,
	                                       UUID uuid,
	                                       @Nullable Text name,
	                                       double percentage,
	                                       BossBar.Color color,
	                                       BossBar.Style style,
	                                       RowType rowType,
	                                       float verticalScale,
	                                       boolean visible) {
		ServerBarSection section = new ServerBarSection(
			container,
			uuid,
			name == null ? Text.empty() : name,
			Math.clamp(percentage,0.0,1.0),
			color,
			style,
			rowType,
			Math.clamp(verticalScale,name == null || name.getString().isBlank() ? 5 : 9,20)
		);
		section.setVisible( visible );
		container.addSection( section );
		return section;
	}

	public MultibarContainer() { this(UUID.randomUUID()); }

	public MultibarContainer(UUID uuid) { this.uuid = uuid; this.displayName = Text.empty(); }

	public MultibarContainer(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
		this(uuid);
		this.darkenSky = darkenSky;
		this.dragonMusic = dragonMusic;
		this.thickenFog = thickenFog;
	}
	public MultibarContainer(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog, boolean visible) {
		this(uuid,darkenSky,dragonMusic,thickenFog);
		this.visible = visible;
	}
	public MultibarContainer(UUID uuid, Text text, boolean darkenSky, boolean dragonMusic, boolean thickenFog, boolean visible) {
		this(uuid,darkenSky,dragonMusic,thickenFog,visible);
		this.displayName = text;
	}
	public boolean isThickenFog() {
		return thickenFog;
	}

	public void setThickenFog(boolean thickenFog) {
		this.thickenFog = thickenFog;
	}

	public boolean isDragonMusic() {
		return dragonMusic;
	}

	public void setDragonMusic(boolean dragonMusic) {
		this.dragonMusic = dragonMusic;
	}

	public boolean isDarkenSky() {
		return darkenSky;
	}

	public void setDarkenSky(boolean darkenSky) {
		this.darkenSky = darkenSky;
	}

	public UUID getUuid() {
		return uuid;
	}

	public boolean addSection(BarSection section) {
		return this.barSectionList.add( section );
	}

	public boolean removeSection(BarSection section) {
		return this.barSectionList.removeIf( s-> s.equals(section) || s.getUuid().equals(section.getUuid()) );
	}

	public boolean clearSections() {
		int prevSize = this.barSectionList.size();
		this.barSectionList.clear();
		return prevSize > 0;
	}

	public void discard() {}

	public Optional<BarSection> getOptionalSection(UUID sectionUUID) {
		return this.barSectionList.stream()
			.filter(f->f.getUuid().equals(sectionUUID))
			.findFirst();
	}

	public boolean isVisible() {
		if (this.barSectionList.stream().noneMatch(BarSection::isVisible)) return false;
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isEmpty() {
		return this.barSectionList.isEmpty();
	}

	public Text getDisplayName() {
		if (this.displayName != null) return this.displayName;
		return this.displayName = Text.empty();
	}

	public void setDisplayName(Text displayName) {
		this.displayName = displayName;
	}

	public List<BarSection> getBars() {
		return this.barSectionList;
	}
}
