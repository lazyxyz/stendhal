/***************************************************************************
 *                   (C) Copyright 2003-2023 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.actions;

import static games.stendhal.common.constants.Actions.ACCEPT;
import static games.stendhal.common.constants.Actions.ACTION;
import static games.stendhal.common.constants.Actions.CHALLENGE;
import static games.stendhal.common.constants.Actions.TARGET;

import org.apache.log4j.Logger;

import games.stendhal.server.core.events.TurnNotifier;
import games.stendhal.server.core.rp.pvp.PlayerVsPlayerChallengeAcceptedTurnListener;
import games.stendhal.server.core.rp.pvp.PlayerVsPlayerChallengeCreatorTurnListener;
import games.stendhal.server.entity.Entity;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.util.EntityHelper;
import marauroa.common.game.RPAction;
/**
 * Handles challenge send from a player's client and creates the server side challenge
 *
 * @author markus
 */
public class ChallengePlayerAction implements ActionListener {

	private static final Logger logger = Logger.getLogger(ChallengePlayerAction.class);

	/**
	 * registers the ChallengePlayerAction action
	 */
	public static void register() {
		CommandCenter.register(CHALLENGE, new ChallengePlayerAction());
	}

	@Override
	public void onAction(Player player, RPAction action) {

		String target = action.get(TARGET);
		Entity targetEntity = EntityHelper.entityFromTargetName(target, player);
		String challengeAction = action.get(ACTION);

		if (targetEntity == null) {
			logger.debug(String.format("Unable to locate target %s for challenge action from player %s", target, player.getName()));
			return;
		}

		if (!(targetEntity instanceof Player)){
			logger.debug(String.format("Found target for name %s is not a player object.", target));
			return;
		}

		Player targetPlayer = (Player) targetEntity;

		if(target != null && target.equals(player.getName())) {
			logger.debug(String.format("Player %s tried to open or accept a challenge with himself.", player.getName()));
			return;
		}

		if("open".equals(challengeAction)) {
			TurnNotifier.get().notifyInTurns(0, new PlayerVsPlayerChallengeCreatorTurnListener(player, targetPlayer));
			return;
		}

		if(ACCEPT.equals(challengeAction)) {
			TurnNotifier.get().notifyInTurns(0, new PlayerVsPlayerChallengeAcceptedTurnListener(targetPlayer, player));
			return;
		}
	}

}
