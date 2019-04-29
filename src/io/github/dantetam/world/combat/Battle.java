package io.github.dantetam.world.combat;

public class Battle {

	public LocalGrid grid;
	public Vector3i battleCenter;
	public List<LivingEntity> combatants;
	public BattleMode battlePhase;
	public int battlePhaseTicksLeft;
	
	public static enum BattleMode {
		PREPARE, SHOCK, DANCE
	}
	
}
