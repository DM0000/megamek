/*
 * MegaMek - Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package megamek.common.weapons.infantry;

import megamek.common.AmmoType;


public class InfantryPulseLaserRifleTirbuni extends InfantryWeapon {

    private static final long serialVersionUID = 1L; // Update for each unique class

    public InfantryPulseLaserRifleTirbuni() {
        super();

        name = "Pulse Laser Rifle (Tirbuni)";
        setInternalName(name);
        addLookupName("TIRBUNI");
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 1900;
        bv = 0.9;
        tonnage = 0.0074;
        infantryDamage = 0.56;
        infantryRange = 4;
        shots = 8;
        bursts = 5;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_LASER).or(F_ENERGY);
        rulesRefs = "Shrapnel #9";

        techAdvancement
                .setTechBase(TechBase.IS)
                .setTechRating(TechRating.D) // Assuming E-E-D-D simplifies to D
                .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(DATE_NONE, DATE_NONE, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(Faction.MH);
    }
}
