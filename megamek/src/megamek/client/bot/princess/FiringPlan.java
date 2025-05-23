/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

import java.io.Serial;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import megamek.codeUtilities.StringUtility;
import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;

/**
 * FiringPlan is a series of {@link WeaponFireInfo} objects describing a full attack turn
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:20 PM
 */
public class FiringPlan extends ArrayList<WeaponFireInfo> implements Comparable<FiringPlan> {
    @Serial
    private static final long serialVersionUID = 8938385222775928559L;

    private double utility; // calculated elsewhere
    private Targetable target;
    private int twist;
    private boolean flipArms;

    FiringPlan() {
        setTwist(0);
        setUtility(0);
    }

    FiringPlan(Targetable target) {
        this();
        this.target = target;
    }

    FiringPlan(Targetable target, boolean flipArms) {
        this(target);
        this.flipArms = flipArms;
    }

    @Override
    public FiringPlan clone() {
        FiringPlan firingPlan = (FiringPlan) super.clone();
        firingPlan.utility = this.utility;
        firingPlan.twist = this.twist;
        firingPlan.flipArms = this.flipArms;
        firingPlan.target = this.target;
        return firingPlan;
    }

    /**
     * @return The total heat for all weapons being fired with this plan.
     */
    synchronized int getHeat() {
        int heat = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            heat += weaponFireInfo.getHeat();
        }
        return heat;
    }

    /**
     * @return The amount of damage based on the damage of each weapon and their odds of hitting.
     */
    synchronized double getExpectedDamage() {
        double expectedDamage = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            expectedDamage += weaponFireInfo.getDamageOnHit() * weaponFireInfo.getProbabilityToHit();
        }
        return expectedDamage;
    }

    synchronized double getExpectedFriendlyDamage() {
        double expectedDamage = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            expectedDamage += weaponFireInfo.getMaxFriendlyDamage() * weaponFireInfo.getProbabilityToHit();
        }
        return expectedDamage;
    }

    synchronized double getExpectedBuildingDamage() {
        double expectedDamage = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            expectedDamage += weaponFireInfo.getMaxBuildingDamage() * weaponFireInfo.getProbabilityToHit();
        }
        return expectedDamage;
    }

    /**
     * @return The total number of expected critical hits based on the chance to hit, damage to target, toughness of
     *       target and odds of rolling a successful critical check.   This is in the units of critical hits.
     */
    synchronized double getExpectedCriticals() {
        double expectedCriticals = 0;
        for (WeaponFireInfo weaponFireInfo : this) {
            expectedCriticals += weaponFireInfo.getExpectedCriticals();
        }
        return expectedCriticals;
    }

    /**
     * Models the probability of each individual weapon getting a kill shot. We treat each weapon shot as a Bernoulli
     * trial and compute the probability of the target surviving each shot.  We can then take 1 - surviveChance to get
     * the chance of getting a kill.  This model doesn't take into consideration multiple weapons hitting the same
     * location.
     *
     * @return The odds of getting a kill based on the odds of each individual weapon getting a kill.  The result will
     *       be between 0 and 1.
     */
    synchronized double getKillProbability() {
        double surviveProbability = 1;

        for (WeaponFireInfo weaponFireInfo : this) {
            surviveProbability *= 1 - weaponFireInfo.getKillProbability();
        }
        return 1 - surviveProbability;
    }

    /**
     * Searches the list of weapons contained in this plan to see if the given weapon is part of it.
     *
     * @param weapon The weapon being searched for.
     *
     * @return TRUE if the given weapon is part of this plan.
     */
    synchronized boolean containsWeapon(Mounted<?> weapon) {
        for (WeaponFireInfo weaponFireInfo : this) {
            if (weaponFireInfo.getWeapon() == weapon) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a {@link Vector} of all the actions, {@link EntityAction}, that make up this firing plan.
     *
     * @return The list of actions as a vector.
     */
    synchronized Vector<EntityAction> getEntityActionVector() {
        Vector<EntityAction> actionVector = new Vector<>();
        if (size() == 0) {
            return actionVector;
        }

        if (getTwist() != 0) {
            actionVector.add(new TorsoTwistAction(get(0).getShooter().getId(),
                  FireControl.correctFacing(get(0).getShooter().getFacing() + getTwist())));
        }

        if (flipArms) {
            actionVector.addElement(new FlipArmsAction(get(0).getShooter().getId(), flipArms));
        }

        for (WeaponFireInfo weaponFireInfo : this) {
            actionVector.add(weaponFireInfo.getWeaponAttackAction());
        }
        return actionVector;
    }

    /*
     * Returns a string describing the firing actions, their likelihood to hit, and damage
     */
    String getDebugDescription(boolean detailed) {
        if (size() == 0) {
            return "Empty FiringPlan!";
        }

        StringBuilder description = new StringBuilder("Firing Plan for ").append(get(0).getShooter().getChassis())
                                          .append(" at ");
        Set<Integer> targets = new HashSet<>();
        // loop through all the targets for this firing plan, only show each target once.
        for (WeaponFireInfo weaponFireInfo : this) {
            if (!targets.contains(weaponFireInfo.getTarget().getId())) {
                description.append(weaponFireInfo.getTarget().getDisplayName()).append(", ");
                targets.add(weaponFireInfo.getTarget().getId());
            }
        }

        // chop off the last ", "
        description.deleteCharAt(description.length() - 1);
        description.deleteCharAt(description.length() - 1);

        description.append("; ").append(size()).append(" weapons fired ");

        if (detailed) {
            for (WeaponFireInfo weaponFireInfo : this) {
                description.append("\n\t\t").append(weaponFireInfo.getDebugDescription());
            }
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.00000");
        description.append("\n\tTotal Expected Damage=").append(decimalFormat.format(getExpectedDamage()));
        description.append("\n\tTotal Expected Criticals=").append(decimalFormat.format(getExpectedCriticals()));
        description.append("\n\tKill Probability=").append(decimalFormat.format(getKillProbability()));
        description.append("\n\tUtility=").append(decimalFormat.format(getUtility()));
        return description.toString();
    }

    public double getUtility() {
        return utility;
    }

    public void setUtility(double utility) {
        this.utility = utility;
    }

    public int getTwist() {
        return twist;
    }

    public void setTwist(int twist) {
        this.twist = twist;
    }

    public boolean getFlipArms() {
        return flipArms;
    }

    public void setFlipArms(boolean flipArms) {
        this.flipArms = flipArms;
    }

    /**
     * @return Who is being shot at?
     */
    public Targetable getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof FiringPlan)) {
            return false;
        } else if (!super.equals(o)) {
            return false;
        }

        FiringPlan that = (FiringPlan) o;

        final double TOLERANCE = 0.00001;
        if (twist != that.twist) {
            return false;
        } else if (Math.abs(utility - that.utility) > TOLERANCE) {
            return false;
        } else if (!target.equals(that.target)) {
            return false;
        } else if (getHeat() != that.getHeat()) {
            return false;
        } else if (Math.abs(getKillProbability() - that.getKillProbability()) > TOLERANCE) {
            return false;
        } else if (Math.abs(getExpectedCriticals() - that.getExpectedCriticals()) > TOLERANCE) {
            return false;
        } else if (Math.abs(getExpectedDamage() - that.getExpectedDamage()) > TOLERANCE) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        String desc = "Utility: " + utility + " ";
        desc += super.toString();
        return desc;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Double.hashCode(utility);
        result = 31 * result + target.hashCode();
        result = 31 * result + twist;
        return result;
    }

    /**
     * Hole punchers before critical seekers
     */
    void sortPlan() {
        this.sort((o1, o2) -> {
            WeaponMounted weapon1 = o1.getWeapon();
            WeaponMounted weapon2 = o2.getWeapon();

            // Both null, both equal.
            if (weapon1 == null && weapon2 == null) {
                return 0;
            }

            // Not null beats null;
            if (weapon1 == null) {
                return -1;
            }
            if (weapon2 == null) {
                return 1;
            }

            double dmg1 = -1;
            double dmg2 = -1;

            WeaponType weaponType1 = weapon1.getType();
            WeaponType weaponType2 = weapon2.getType();

            AmmoMounted ammo1 = weapon1.getLinkedAmmo();
            AmmoMounted ammo2 = weapon2.getLinkedAmmo();

            dmg1 = getDamageByClusterTable(dmg1, weaponType1, ammo1);
            dmg2 = getDamageByClusterTable(dmg2, weaponType2, ammo2);

            return -Double.compare(dmg1, dmg2);
        });
    }

    private double getDamageByClusterTable(double damage, WeaponType weaponType, AmmoMounted ammoMounted) {
        if (ammoMounted != null) {
            AmmoType ammoType = ammoMounted.getType();
            if ((WeaponType.DAMAGE_BY_CLUSTERTABLE == weaponType.getDamage()) ||
                      (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                damage = ammoType.getDamagePerShot();
            }
        }

        if (damage == -1) {
            damage = weaponType.getDamage();
        }
        
        return damage;
    }

    String getWeaponNames() {
        StringBuilder out = new StringBuilder();
        for (WeaponFireInfo wfi : this) {
            if (!StringUtility.isNullOrBlank(out)) {
                out.append(",");
            }

            if (wfi.getWeapon() == null) {
                out.append("null");
                continue;
            }
            out.append(wfi.getWeapon().getName());
        }
        return out.toString();
    }

    /**
     * Compares to FiringPlans based on utility.  Higher utility is better.
     */
    @Override
    public int compareTo(FiringPlan other) {
        return (int) (getUtility() - other.getUtility() + 0.5);
    }
}
