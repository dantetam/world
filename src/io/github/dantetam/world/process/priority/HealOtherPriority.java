package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.life.BodyPart;
import io.github.dantetam.world.life.LivingEntity;

public class HealOtherPriority extends Priority {

	public LivingEntity doctor, patient;
	public BodyPart part;
	public boolean patientConsents;
	
	public HealOtherPriority(Vector3i coords, LivingEntity doctor, LivingEntity patient, 
			boolean patientConsents) {
		super(coords);
		this.doctor = doctor;
		this.patient = patient;
		this.patientConsents = patientConsents;
	}
	
}
