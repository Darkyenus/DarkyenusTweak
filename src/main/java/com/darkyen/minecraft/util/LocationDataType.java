package com.darkyen.minecraft.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.darkyen.minecraft.util.Util.getWorld;

/**
 *
 */
public class LocationDataType implements PersistentDataType<PersistentDataContainer, Location> {
	private final NamespacedKey WORLD_UUID_LOW;
	private final NamespacedKey WORLD_UUID_HIGH;
	private final NamespacedKey POS_X;
	private final NamespacedKey POS_Y;
	private final NamespacedKey POS_Z;

	public LocationDataType(Plugin plugin) {
		WORLD_UUID_LOW = new NamespacedKey(plugin, "w_l");
		WORLD_UUID_HIGH = new NamespacedKey(plugin, "w_h");
		POS_X = new NamespacedKey(plugin, "x");
		POS_Y = new NamespacedKey(plugin, "y");
		POS_Z = new NamespacedKey(plugin, "z");
	}

	@Override
	public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
		return PersistentDataContainer.class;
	}

	@Override
	public @NotNull Class<Location> getComplexType() {
		return Location.class;
	}

	@NotNull
	@Override
	public PersistentDataContainer toPrimitive(@NotNull Location complex, @NotNull PersistentDataAdapterContext context) {
		final PersistentDataContainer container = context.newPersistentDataContainer();
		final World world = getWorld(complex);
		if (world != null) {
			final UUID uid = world.getUID();
			container.set(WORLD_UUID_LOW, PersistentDataType.LONG,  uid.getLeastSignificantBits());
			container.set(WORLD_UUID_HIGH, PersistentDataType.LONG,  uid.getMostSignificantBits());
		}
		container.set(POS_X, PersistentDataType.DOUBLE, complex.getX());
		container.set(POS_Y, PersistentDataType.DOUBLE, complex.getY());
		container.set(POS_Z, PersistentDataType.DOUBLE, complex.getZ());
		return container;
	}

	@NotNull
	@Override
	public Location fromPrimitive(@NotNull PersistentDataContainer primitive, @NotNull PersistentDataAdapterContext context) {
		final Long worldUUIDLow = primitive.get(WORLD_UUID_LOW, PersistentDataType.LONG);
		final Long worldUUIDHigh = primitive.get(WORLD_UUID_HIGH, PersistentDataType.LONG);
		final World world;
		if (worldUUIDLow != null && worldUUIDHigh != null) {
			world = Bukkit.getWorld(new UUID(worldUUIDHigh, worldUUIDLow));
		} else {
			world = null;
		}

		final double x = primitive.getOrDefault(POS_X, PersistentDataType.DOUBLE, 0.0);
		final double y = primitive.getOrDefault(POS_Y, PersistentDataType.DOUBLE, 0.0);
		final double z = primitive.getOrDefault(POS_Z, PersistentDataType.DOUBLE, 0.0);
		return new Location(world, x, y, z);
	}
}
