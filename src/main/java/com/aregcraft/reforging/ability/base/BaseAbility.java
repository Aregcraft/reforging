package com.aregcraft.reforging.ability.base;

import com.aregcraft.reforging.ability.external.Function;
import com.aregcraft.reforging.ability.external.Price;
import com.aregcraft.reforging.math.Matrix;
import com.aregcraft.reforging.math.Vector;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public abstract class BaseAbility {
    public static final BaseAbility DUMMY = new BaseAbility() {
        @Override
        public void activate(Player player) {
        }
    };
    private Price price;

    protected Matrix changeOfBasisDirection(Player player) {
        var z = new Vector(player.getLocation().getDirection());
        var x = z.cross(new Vector(0, 1, 0));
        var y = x.cross(z);
        return Matrix.changeOfBasis(x, y, z);
    }

    protected Vector evaluate(Function function, double t) {
        var x = 0.0;
        var y = 0.0;
        var z = 0.0;
        if (function.x != null) {
            function.x.setArgumentValue("t", t);
            x = function.x.calculate();
        }
        if (function.y != null) {
            function.y.setArgumentValue("t", t);
            y = function.y.calculate();
        }
        if (function.z != null) {
            function.z.setArgumentValue("t", t);
            z = function.z.calculate();
        }
        return new Vector(x, y, z);
    }

    protected <T extends Entity> T spawnEntity(Vector vector, Class<T> entityType, Location location) {
        var world = location.getWorld();
        if (world == null) {
            return null;
        }
        return world.spawn(vector.at(location), entityType);
    }

    protected void spawnParticle(Vector vector, Particle particle, Location location) {
        var world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(particle, vector.at(location), 0);
    }

    protected ArmorStand spawnArmorStand(Location location, ItemStack item) {
        var armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setArms(true);
        armorStand.getEquipment().setItemInMainHand(item);
        return armorStand;
    }

    protected void charge(Player player) {
        player.damage(price.health);
        player.setFoodLevel(Math.max(player.getFoodLevel() - price.food, 0));
    }

    protected void forEachEntity(Location location, Player player, Consumer<LivingEntity> action) {
        location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5).stream()
                .filter(it -> !it.equals(player)).filter(it -> it instanceof LivingEntity)
                .map(it -> (LivingEntity) it).forEach(action);
    }

    protected boolean isUnfilled(Location location) {
        return !location.getBlock().getType().isSolid();
    }

    public abstract void activate(Player player);
}