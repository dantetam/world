package io.github.dantetam.world.ai;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.AlgUtil;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.worldgen.LocalGridTerrainInstantiate;

public class HierarchicalPathfinder extends Pathfinder {

	public static final int ABSTRACT_BLOCK_SIZE = 10;
	private LocalGridBlock[][][] abstractBlocks;
	
	public HierarchicalPathfinder(LocalGrid grid) {
		super(grid);
		int rows = (int) Math.ceil((double) grid.rows / ABSTRACT_BLOCK_SIZE);
		int cols = (int) Math.ceil((double) grid.cols / ABSTRACT_BLOCK_SIZE);
		int heights = (int) Math.ceil((double) grid.heights / ABSTRACT_BLOCK_SIZE);
		abstractBlocks = new LocalGridBlock[rows][cols][heights];
		initAllAbstractNodes();
	}
	
	private void initAllAbstractNodes() {
		for (int r = 0; r < abstractBlocks.length; r++) {
			for (int c = 0; c < abstractBlocks[0].length; c++) {
				for (int h = 0; h < abstractBlocks[0][0].length; h++) {
					Vector3i minB = new Vector3i(ABSTRACT_BLOCK_SIZE * r, ABSTRACT_BLOCK_SIZE * c, ABSTRACT_BLOCK_SIZE * h);
					Vector3i maxB = new Vector3i(
							ABSTRACT_BLOCK_SIZE * (r + 1) - 1, 
							ABSTRACT_BLOCK_SIZE * (c + 1) - 1, 
							ABSTRACT_BLOCK_SIZE * (h + 1) - 1
							);
					abstractBlocks[r][c][h] = new LocalGridBlock(minB, maxB);
				}
			}
		}
		
		for (int r = 0; r < abstractBlocks.length; r++) {
			for (int c = 0; c < abstractBlocks[0].length; c++) {
				for (int h = 0; h < abstractBlocks[0][0].length; h++) {
					LocalGridBlock block = abstractBlocks[r][c][h];
					
					getAllMaximalDoors(new Vector3i(r,c,h), 'r', true);
					getAllMaximalDoors(new Vector3i(r,c,h), 'r', false);
					getAllMaximalDoors(new Vector3i(r,c,h), 'c', true);
					getAllMaximalDoors(new Vector3i(r,c,h), 'c', false);
					getAllMaximalDoors(new Vector3i(r,c,h), 'h', true);
					getAllMaximalDoors(new Vector3i(r,c,h), 'h', false);
					
					getDistInAbsBlock(block);
					
					System.out.println("Completed abstraction at block coord: " + new Vector3i(r,c,h));
				}
			}
		}
	}
	
	public void getDistInAbsBlock(LocalGridBlock block) {
		System.out.println("Computing paths between num blocks: " + block.importantNodes.size());
		
		for (AbstractNode node: block.importantNodes) {
			for (AbstractNode otherNode: block.importantNodes) {
				if (node.equals(otherNode)) continue;
				ScoredPath path = super.findPath(null, 
						this.grid.getTile(node.coords), this.grid.getTile(otherNode.coords),
						block.minBound, block.maxBound
						);
				if (path != null)
					node.addNode(otherNode, path.score);
			}
			if (node.mirrorConnection != null) { 
				System.out.println(node.coords + " connected " + node.mirrorConnection.coords);
				ScoredPath path = super.findPath(null, 
						this.grid.getTile(node.coords), this.grid.getTile(node.mirrorConnection.coords));
				if (path != null)
					node.addNode(node.mirrorConnection, path.score);
			}
		}
	}
	
	public void getAllMaximalDoors(Vector3i blockCoords, 
			char dimension, boolean positive) {
		boolean[][] firstBlockAccess = new boolean[ABSTRACT_BLOCK_SIZE][ABSTRACT_BLOCK_SIZE],
				secondBlockAccess = new boolean[ABSTRACT_BLOCK_SIZE][ABSTRACT_BLOCK_SIZE];
		
		LocalGridBlock mainBlock = abstractBlocks[blockCoords.x][blockCoords.y][blockCoords.z]; 
			
		int firstR;
		if (dimension == 'r') {
			firstR = positive ? mainBlock.maxBound.x : mainBlock.minBound.x;
		} else if (dimension == 'c') {
			firstR = positive ? mainBlock.maxBound.y : mainBlock.minBound.y; 
		} else {
			firstR = positive ? mainBlock.maxBound.z : mainBlock.minBound.z; 
		}
		int offset = positive ? 1 : -1;
		int	secondR = firstR + offset;
		LocalGridBlock otherBlock; 
		try {
			if (dimension == 'r') {
				otherBlock = abstractBlocks[blockCoords.x + offset][blockCoords.y][blockCoords.z]; 
			} else if (dimension == 'c') {
				otherBlock = abstractBlocks[blockCoords.x][blockCoords.y + offset][blockCoords.z]; 
			} else {
				otherBlock = abstractBlocks[blockCoords.x][blockCoords.y][blockCoords.z + offset]; 
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return;
		}
		for (int c = 0; c < ABSTRACT_BLOCK_SIZE; c++) {
			for (int h = 0; h < ABSTRACT_BLOCK_SIZE; h++) {
				if (dimension == 'r') {
					System.out.println(new Vector3i(
							firstR,
							mainBlock.minBound.y + c,
							mainBlock.minBound.z + h
							));
					firstBlockAccess[c][h] = !this.grid.tileIsOccupied(new Vector3i(
							firstR,
							mainBlock.minBound.y + c,
							mainBlock.minBound.z + h
							));
					secondBlockAccess[c][h] = !this.grid.tileIsOccupied(new Vector3i(
							secondR,
							mainBlock.minBound.y + c,
							mainBlock.minBound.z + h
							));
				} else if (dimension == 'c') {
					firstBlockAccess[c][h] = !this.grid.tileIsOccupied(new Vector3i(
							mainBlock.minBound.x + c,
							firstR,
							mainBlock.minBound.z + h
							));
					secondBlockAccess[c][h] = !this.grid.tileIsOccupied(new Vector3i(
							mainBlock.minBound.x + c,
							secondR,
							mainBlock.minBound.z + h
							));
				} else {
					firstBlockAccess[c][h] = !this.grid.tileIsOccupied(new Vector3i(
							mainBlock.minBound.x + c,
							mainBlock.minBound.y + h,
							firstR
							));
					secondBlockAccess[c][h] = !this.grid.tileIsOccupied(new Vector3i(
							mainBlock.minBound.x + c,
							mainBlock.minBound.y + h,
							secondR
							));
				}
				

				System.out.println("---------------");
				for (int i = 0; i < firstBlockAccess.length; i++) {
					for (int j = 0; j < firstBlockAccess[0].length; j++) {
						System.out.print(firstBlockAccess[i][j] ? "X" : "_");
					}
					System.out.println();
				}
				
				//Compute the maximal doors, where the blocks' borders open into the largest shared 2d spaces
				List<Set<Vector2i>> maximalDoorsLocalCoord = findMaximalDoorBool(
						firstBlockAccess, secondBlockAccess);
				for (Set<Vector2i> compLocalCoord: maximalDoorsLocalCoord) {
					//Convert the maximal door into an outline, and then retrieve its corners
					//The corners represent the abstract nodes in the pre-calculation,
					//which are always shared between the two blocks in question.
					Set<Vector2i> borderComp = AlgUtil.getBorderRegionFromCoords2d(compLocalCoord);
					Set<Vector2i> corners = AlgUtil.getCornerRegionCoords2d(borderComp);
					
					List<Vector3i> trueCoordsFirst = new ArrayList<>();
					List<Vector3i> trueCoordsSecond = new ArrayList<>();
					
					for (Vector2i corner: corners) {
						//Convert the corners into real-world coordinates and store them for future use,
						//while pre-calculating the distance.
						//We rely on the iff relationship that,
						//a maximal door is a unique connected component <-> 
						//corners of the door cannot give a valid path outside of the door 
						Vector3i trueCoordinateFirst, trueCoordinateSecond;
						if (dimension == 'r') {
							trueCoordinateFirst = new Vector3i(
									firstR,
									mainBlock.minBound.y + corner.x,
									mainBlock.minBound.z + corner.y
									);
							trueCoordinateSecond = new Vector3i(
									secondR,
									mainBlock.minBound.y + corner.x,
									mainBlock.minBound.z + corner.y
									);
						} else if (dimension == 'c') {
							trueCoordinateFirst = new Vector3i(
									mainBlock.minBound.x + corner.x,
									firstR,
									mainBlock.minBound.z + corner.y
									);
							trueCoordinateSecond = new Vector3i(
									mainBlock.minBound.x + corner.x,
									secondR,
									mainBlock.minBound.z + corner.y
									); 
						} else {
							trueCoordinateFirst = new Vector3i(
									mainBlock.minBound.x + corner.x,
									mainBlock.minBound.y + corner.y,
									firstR
									);
							trueCoordinateSecond = new Vector3i(
									mainBlock.minBound.x + corner.x,
									mainBlock.minBound.y + corner.y,
									secondR
									);
						}
						
						trueCoordsFirst.add(trueCoordinateFirst);
						trueCoordsSecond.add(trueCoordinateSecond);
					}
					
					System.out.println(dimension);
					System.out.println(blockCoords.z + " " + offset);
					System.out.println(trueCoordsFirst);
					System.out.println(trueCoordsSecond);
					
					for (int coordIndex = 0; coordIndex < trueCoordsFirst.size(); coordIndex++) {
						AbstractNode node1 = new AbstractNode(trueCoordsFirst.get(coordIndex));
						mainBlock.importantNodes.add(node1);
						
						AbstractNode node2 = new AbstractNode(trueCoordsSecond.get(coordIndex));
						otherBlock.importantNodes.add(node2);
						
						node1.mirrorConnection = node2;
						node2.mirrorConnection = node1;
					}
				}
			}
		}
	}
	
	//Given two equally sized 2d arrays of booleans, find the maximal doors,
	//in 2d coordinates grouped together as cluster.
	private List<Set<Vector2i>> findMaximalDoorBool(boolean[][] first, boolean[][] second) {
		if (first.length != second.length || first[0].length != second.length) {
			throw new IllegalArgumentException("Cannot calculate maximal door boolean data on mismatched dimensions of 2d arrays");
		}
		boolean[][] overlap = new boolean[first.length][first[0].length];
		for (int i = 0; i < first.length; i++) {
			for (int j = 0; j < first[0].length; j++) {
				overlap[i][j] = first[i][j] && second[i][j];
			}
		}
		return AlgUtil.getConnectedComponents(overlap, false);
	}
	
	private static class LocalGridBlock {
		public Vector3i minBound, maxBound;
		public Set<AbstractNode> importantNodes;
		
		public LocalGridBlock(Vector3i minB, Vector3i maxB) {
			this.minBound = minB;
			this.maxBound = maxB;
			importantNodes = new HashSet<>();
		}
	}
	
	private static class AbstractNode {
		public Vector3i coords;
		
		private Map<AbstractNode, Double> distToPathableNodes;
		
		public AbstractNode mirrorConnection;
		
		public AbstractNode(Vector3i coords) {
			this.coords = coords;
		}
		
		public double getDist(AbstractNode otherNode) {
			return distToPathableNodes.get(otherNode);
		}
		
		public void addNode(AbstractNode node, double dist) {
			if (distToPathableNodes == null) {
				distToPathableNodes = new HashMap<>();
			}
			distToPathableNodes.put(node, dist);
		}
		
		public String toString() {
			return "Coords: " + coords.toString() + ", distances: " + distToPathableNodes.toString();
		}
		
		@Override
		public int hashCode() {
			return coords.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AbstractNode other = (AbstractNode) obj;
			if (coords == null) {
				if (other.coords != null)
					return false;
			} else if (!coords.equals(other.coords))
				return false;
			return true;
		}	
	}
	
	public static void main(String[] args) {
		WorldCsvParser.init();
    	
    	Vector3i sizes = new Vector3i(50,50,50);
		int biome = 3;
		LocalGrid activeLocalGrid = new LocalGridTerrainInstantiate(sizes, biome).setupGrid();
		
		Society testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(10,10,10);
		
		HierarchicalPathfinder hPath = new HierarchicalPathfinder(activeLocalGrid);
		
		for (int r = 0; r < hPath.abstractBlocks.length; r++) {
			for (int c = 0; c < hPath.abstractBlocks[0].length; c++) {
				for (int h = 0; h < hPath.abstractBlocks[0][0].length; h++) {
					LocalGridBlock block = hPath.abstractBlocks[r][c][h];
					System.out.println(block + "--------------------");
					System.out.println("Local Block Bounds: " + block.minBound + " -> " + block.maxBound);
					for (AbstractNode node: block.importantNodes) {
						System.out.println("\t" + node.toString());
					}
				}
			}
		}
	}
	
}
