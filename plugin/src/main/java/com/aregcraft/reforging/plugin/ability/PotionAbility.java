package com.aregcraft.reforging.plugin.ability;

import com.aregcraft.reforging.core.item.PotionItemWrapper;
import com.aregcraft.reforging.plugin.ability.base.ProjectileAbility;
import com.aregcraft.reforging.plugin.annotation.Ability;
import org.bukkit.Material;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Allows the player to throw a potion.
 */
@Ability
public class PotionAbility extends ProjectileAbility<ThrownPotion> {
    /**
     * Specifies the effect of the thrown potion.
     */
    private PotionEffectType effect;
    /**
     * Specifies the duration of the effect in ticks (1 second = 20 ticks).
     */
    private int duration;
    /**
     * Specifies the amplifier of the effect.
     */
    private int amplifier;

    protected PotionAbility() {
        super(ThrownPotion.class);
    }

    @Override
    protected void configure(ThrownPotion projectile) {
        var item = PotionItemWrapper.create(Material.SPLASH_POTION);
        item.addEffect(new PotionEffect(effect, duration, amplifier));
        projectile.setItem(item.unwrap());
    }
}
