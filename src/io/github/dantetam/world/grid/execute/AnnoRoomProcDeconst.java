package io.github.dantetam.world.grid.execute;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.org.apache.xpath.internal.functions.Function;

import java.util.Map.Entry;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civhumanai.NeedsGamut;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.civilization.gridstructure.AnnotatedRoom;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnoBuildDesPriority;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnotatedBuild;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.ItemMetricsUtil;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.SpaceFillingAlg;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.priority.ConstructRoomPriority;
import io.github.dantetam.world.process.priority.ImpossiblePriority;
import io.github.dantetam.world.process.priority.ImproveRoomPriority;
import io.github.dantetam.world.process.priority.Priority;

public class AnnoRoomProcDeconst {

	public static Priority improveComplex(LocalGrid grid, Society society, Human being, Human ownerProducts,
			Set<Human> validLandOwners, Set<LivingEntity> validEntities, NeedsGamut humanNeeds) {
		Priority priority = null;
		
		Entry<PurposeAnnotatedBuild, AnnotatedRoom> bestEntry = PurposeAnnoBuildDesPriority.
				futureRoomNeedByScore(being, humanNeeds);
		if (bestEntry == null) return new ImpossiblePriority("Could not find complex to improve");
		PurposeAnnotatedBuild complex = bestEntry.getKey();
		AnnotatedRoom room = bestEntry.getValue();
		
		//Once in a while, expand the annotated complex into new tiles
		if (Math.random() < 0.2) {
			GridRectInterval bestSingleRect = SpaceFillingAlg.expandAnnotatedComplex(grid, being.society,
					validLandOwners, complex, room);
			List<GridRectInterval> bestRectangles = new ArrayList<GridRectInterval>() {{
					add(bestSingleRect); 
					}};
					
			LinkedHashSet<Vector3i> borderRegion = VecGridUtil.getBorderRegionFromCoords(
					Vector3i.getRange(bestRectangles));
			Set<Integer> bestBuildingMaterials = society.getBestBuildingMaterials(society.calcUtility, 
					being, borderRegion.size());
				
			if (bestSingleRect != null) {
				//Use land claims on the spaces not already claimed
				for (GridRectInterval interval: bestRectangles) {
					List<Human> claimants = grid.findClaimantToTiles(interval);
					if (claimants == null || claimants.size() == 0) {
						grid.claimTiles(ownerProducts, interval.getStart(), interval.getEnd(), null);
					}
				}
				
				complex.addRoom(room.purpose, bestRectangles, borderRegion, 
						VecGridUtil.setUnderVecs(grid, Vector3i.getRange(bestRectangles)));
				MapUtil.insertNestedListMap(ownerProducts.designatedBuildsByPurpose, room.purpose, complex);
				
				priority = new ConstructRoomPriority(borderRegion, bestBuildingMaterials);
			}
			else {
				priority = new ImpossiblePriority("Could not find open rectangular space");
			}
		}
		
		//Otherwise, improve any number of rooms within the complex using simple processes,
		//such as moving items, furniture, building the walls/floors, and so on.
		if (priority == null) {
			priority = new ImproveRoomPriority(room, validEntities);
			/*
			origBuildCounts, desiredBuildCounts; 
			origBuildCountsOpt, desiredBuildCountsOpt;
			origItemStorage, desiredItemStorage;
			origItemStorageOpt, desiredItemStorageOpt;
			if (desiredBuildCounts)
			*/
			
		}
		
		return priority;
	}
	
}
