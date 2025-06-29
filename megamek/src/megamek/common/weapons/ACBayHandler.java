/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.WeaponMounted;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class ACBayHandler extends AmmoBayWeaponHandler {

    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public ACBayHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            WeaponType bayWType = bayW.getType();
            int ammoUsed = bayW.getCurrentShots();
            if (bayWType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ROTARY) {
                boolean jams = false;
                switch (ammoUsed) {
                    case 6:
                        if (roll.getIntValue() <= 4) {
                            jams = true;
                        }
                        break;
                    case 5:
                    case 4:
                        if (roll.getIntValue() <= 3) {
                            jams = true;
                        }
                        break;
                    case 3:
                    case 2:
                        if (roll.getIntValue() <= 2) {
                            jams = true;
                        }
                        break;
                    default:
                        break;
                }
                if (jams) {
                    Report r = new Report(3170);
                    r.subject = subjectId;
                    r.add(" shot(s)");
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                    bayW.setJammed(true);
                }
            } else if (bayWType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA) {
                if (roll.getIntValue() == 2 && ammoUsed == 2) {
                    Report r = new Report();
                    r.subject = subjectId;
                    r.messageId = 3160;
                    r.newlines = 0;
                    bayW.setJammed(true);
                    bayW.setHit(true);
                    vPhaseReport.addElement(r);
                }
            }
        }

        return false;
    }
}
