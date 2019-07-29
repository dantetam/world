package io.github.dantetam.world.ai;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.WorldCsvParser;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.worldgen.LocalGridBiome;
import io.github.dantetam.world.worldgen.LocalGridInstantiate;

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
	
	public double getNodeDist(AbstractNode c, AbstractNode v) {
		return c.coords.squareDist(v.coords);
	}
	
	public ScoredPath findPath(LivingEntity being, LocalTile tileA, LocalTile tileB) {
		return this.findPath(being, tileA.coords, tileB.coords);
	}
	
	public ScoredPath findPath(LivingEntity being, Vector3i startVec, Vector3i endVec) {
		LocalGridBlock startCluster = getBlockFromCoords(startVec), endCluster = getBlockFromCoords(endVec);
		AbstractNode startNode = addTempNode(startCluster, startVec);
		AbstractNode endNode = addTempNode(endCluster, endVec);
		ScoredAbstractPath absPath = findAbstractPath(startNode, endNode);
		
		List<LocalTile> completePath = new ArrayList<>();
		double totalScore = 0;
		
		if (absPath == null) {
			return new ScoredPath(completePath, totalScore);
		}
		
		//CustomLog.outPrintln(absPath);
		CustomLog.outPrintln(startVec + " path to -> " + endVec);
		for (int node = 0; node < absPath.path.size() - 1; node++) {
			AbstractNode firstNode = absPath.path.get(node);
			AbstractNode secondNode = absPath.path.get(node + 1);
			//CustomLog.outPrintln("\t Score: " + firstNode.distToPathableNodes.get(secondNode).score + ": " + firstNode.distToPathableNodes.get(secondNode).path);
			//ScoredPath localPath = super.findPath(being, grid.getTile(firstNode.coords), grid.getTile(secondNode.coords));
			ScoredPath localPath = firstNode.distToPathableNodes.get(secondNode);
			for (int local = 0; local < localPath.path.size(); local++) {
				//Do not double count vertices due to the way they are compiled in a hierarchical search
				if (node != absPath.path.size() - 2 && local == localPath.path.size() - 1) break;
				LocalTile tile = localPath.path.get(local);
				completePath.add(tile);
			}
			totalScore += localPath.score;
		}
		
		ScoredPath path = new ScoredPath(completePath, totalScore);
		
		removeTempNode(getBlockFromCoords(startVec), startNode);
		removeTempNode(getBlockFromCoords(endVec), endNode);
		
		return path;
	}
	
	public ScoredAbstractPath findAbstractPath(AbstractNode start, AbstractNode end) {	
    	List<AbstractNode> results = new ArrayList<>();
        if (start.equals(end)) {
            results.add(end);
            return new ScoredAbstractPath(results, 0);
        }
        Set<AbstractNode> visited = new HashSet<>();
        final HashMap<AbstractNode, Double> dist = new HashMap<>();
        Map<AbstractNode, AbstractNode> prev = new HashMap<>();
        PriorityQueue<AbstractNode> fringe;
        
        fringe = new PriorityQueue<AbstractNode>(16, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
            	AbstractNode n1 = (AbstractNode) o1;
            	AbstractNode n2 = (AbstractNode) o2;
                if (n1.equals(n2)) return 0;
                return (
                		1 * (dist.get(n1) - dist.get(n2)) + 
                		1.05 * (getNodeDist(end, n1) - getNodeDist(end, n2))
                		) > 0 ? 1 : -1;
            }
        });
        
        fringe.add(start);
        dist.put(start, 0.0);
        while (!fringe.isEmpty()) {        	
        	AbstractNode v = fringe.poll();
        	//CustomLog.outPrintln("At node: " + v.coords.toString());
            if (visited.contains(v)) {
                continue;
            }
            visited.add(v);
            if (v.equals(end)) {
                do {
                    results.add(0, v);
                    v = prev.get(v);
                } while (v != null);
                return new ScoredAbstractPath(results, dist.get(end).doubleValue());
            }
            if (v.distToPathableNodes != null) {
            	for (Entry<AbstractNode, ScoredPath> entry: v.distToPathableNodes.entrySet()) {
            		AbstractNode c = entry.getKey();
            		double cvHeurDist = entry.getValue().score;
                    if ((!dist.containsKey(c)) || (dist.containsKey(c) && dist.get(c) > dist.get(v) + cvHeurDist)) {
                    	//CustomLog.outPrintln("\t Expanding: " + c.coords.toString());
                    	dist.put(c, dist.get(v) + 0.7*cvHeurDist);
                        fringe.add(c);
                        prev.put(c, v);
                    }
            	}
            }
        }
        return null;
    }
	
	private LocalGridBlock getBlockFromCoords(Vector3i coords) {
		int rIndex = coords.x / ABSTRACT_BLOCK_SIZE;
		int cIndex = coords.y / ABSTRACT_BLOCK_SIZE;
		int hIndex = coords.z / ABSTRACT_BLOCK_SIZE;
		if (rIndex < 0 || cIndex < 0 || hIndex < 0 || 
				rIndex >= abstractBlocks.length || 
				cIndex >= abstractBlocks[0].length || 
				hIndex >= abstractBlocks[0][0].length) {
			CustomLog.errPrintln("Warning, vector out of bounds for abstract blocks");
			return null;
		}
		return abstractBlocks[rIndex][cIndex][hIndex];
	}
	
	private void initAllAbstractNodes() {
		for (int r = 0; r < abstractBlocks.length; r++) {
			for (int c = 0; c < abstractBlocks[0].length; c++) {
				for (int h = 0; h < abstractBlocks[0][0].length; h++) {
					Vector3i minB = new Vector3i(
							ABSTRACT_BLOCK_SIZE * r, 
							ABSTRACT_BLOCK_SIZE * c, 
							ABSTRACT_BLOCK_SIZE * h
							);
					Vector3i maxB = new Vector3i(
							ABSTRACT_BLOCK_SIZE * (r + 1) - 1, 
							ABSTRACT_BLOCK_SIZE * (c + 1) - 1, 
							ABSTRACT_BLOCK_SIZE * (h + 1) - 1
							);
					abstractBlocks[r][c][h] = new LocalGridBlock(minB, maxB);
					
					CustomLog.outPrintln(minB + " " + maxB + " <BOunds!");
				}
			}
		}
		
		for (int r = 0; r < abstractBlocks.length; r++) {
			for (int c = 0; c < abstractBlocks[0].length; c++) {
				for (int h = 0; h < abstractBlocks[0][0].length; h++) {
					LocalGridBlock block = abstractBlocks[r][c][h];
					
					//Make this process more efficient by not double using maximal door info
					//Once the maximal 2d window has been calculated between two 3d blocks,
					//use it for both sets of important points.
					
					getAllMaximalDoors(new Vector3i(r,c,h), 'r', true);
					getAllMaximalDoors(new Vector3i(r,c,h), 'r', false);
					getAllMaximalDoors(new Vector3i(r,c,h), 'c', true);
					getAllMaximalDoors(new Vector3i(r,c,h), 'c', false);
					getAllMaximalDoors(new Vector3i(r,c,h), 'h', true);
					getAllMaximalDoors(new Vector3i(r,c,h), 'h', false);
					
					getDistInAbsBlock(block);
					
					CustomLog.outPrintln("Completed abstraction at block coord: " + new Vector3i(r,c,h));
				}
			}
		}
	}
	
	public void getDistInAbsBlock(LocalGridBlock block) {
		CustomLog.outPrintln("Computing paths between num blocks: " + block.importantNodes.size());
		
		//Use a three dimensional flood fill (connected component search)
		//to eliminate any impossible paths immediately
		Map<Vector3i, Integer> connectedCompsMap = VecGridUtil.contComponent3dSolids(
				block.minBound, block.maxBound, grid);
		
		for (AbstractNode node: block.importantNodes.values()) {
			for (AbstractNode otherNode: block.importantNodes.values()) {
				if (connectedCompsMap.containsKey(node.coords) &&
						connectedCompsMap.containsKey(otherNode.coords)) {
					if (connectedCompsMap.get(node.coords) == connectedCompsMap.get(otherNode.coords)) {
						attemptConnectAbsNode(node, otherNode, block);
						boolean success = (node.distToPathableNodes != null && 
								node.distToPathableNodes.containsKey(otherNode));
						if (!success) {
							CustomLog.outPrintln("Connected in 3d component");
							CustomLog.outPrintln("Found path: " + success);
						}
					}
				}
			}
			if (node.mirrorConnections != null) { 
				//CustomLog.outPrintln(node.coords + " connected " + node.mirrorConnection.coords);
				for (AbstractNode mirrorConnection: node.mirrorConnections) {
					attemptConnectAbsNode(node, mirrorConnection, null);
				}
			}
		}
	}
	
	/**
	 * Find a path between node and otherNode. If block is given, restrict the path to within its boundaries
	 * @param node
	 * @param otherNode
	 * @param block      The potential restriction (null if not given)
	 */
	private void attemptConnectAbsNode(AbstractNode node, AbstractNode otherNode, LocalGridBlock block) {
		if (node.equals(otherNode)) return;
		ScoredPath existingPath = otherNode.getDist(node);
		if (existingPath == null) {
			if (block != null) {
				existingPath = super.findPath(null, 
						this.grid.getTile(node.coords), this.grid.getTile(otherNode.coords),
						block.minBound, block.maxBound
						);
			}
			else {
				existingPath = super.findPath(null, 
						this.grid.getTile(node.coords), this.grid.getTile(otherNode.coords));
			}
		}
		if (existingPath != null)
			node.addNode(otherNode, existingPath);
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
		//Set up the two 2d arrays firstBlockAccess, secondBlockAccess, in the space between the two blocks
		for (int c = 0; c < ABSTRACT_BLOCK_SIZE; c++) {
			for (int h = 0; h < ABSTRACT_BLOCK_SIZE; h++) {
				if (dimension == 'r') {
					firstBlockAccess[c][h] = this.grid.tileIsAccessible(new Vector3i(
							firstR,
							mainBlock.minBound.y + c,
							mainBlock.minBound.z + h
							));
					secondBlockAccess[c][h] = this.grid.tileIsAccessible(new Vector3i(
							secondR,
							mainBlock.minBound.y + c,
							mainBlock.minBound.z + h
							));
				} else if (dimension == 'c') {
					firstBlockAccess[c][h] = this.grid.tileIsAccessible(new Vector3i(
							mainBlock.minBound.x + c,
							firstR,
							mainBlock.minBound.z + h
							));
					secondBlockAccess[c][h] = this.grid.tileIsAccessible(new Vector3i(
							mainBlock.minBound.x + c,
							secondR,
							mainBlock.minBound.z + h
							));
				} else {
					firstBlockAccess[c][h] = this.grid.tileIsAccessible(new Vector3i(
							mainBlock.minBound.x + c,
							mainBlock.minBound.y + h,
							firstR
							));
					secondBlockAccess[c][h] = this.grid.tileIsAccessible(new Vector3i(
							mainBlock.minBound.x + c,
							mainBlock.minBound.y + h,
							secondR
							));
				}
			}
		}
		/*
		CustomLog.outPrintln("---------------");
		for (int i = 0; i < firstBlockAccess.length; i++) {
			for (int j = 0; j < firstBlockAccess[0].length; j++) {
				System.out.print(firstBlockAccess[i][j] ? "X" : "_");
			}
			CustomLog.outPrintln();
		}
		*/
		
		//Compute the maximal doors, where the blocks' borders open into the largest shared 2d spaces
		List<Set<Vector2i>> maximalDoorsLocalCoord = findMaximalDoorBool(
				firstBlockAccess, secondBlockAccess);
		for (Set<Vector2i> compLocalCoord: maximalDoorsLocalCoord) {
			//Convert the maximal door into an outline, and then retrieve its corners
			//The corners represent the abstract nodes in the pre-calculation,
			//which are always shared between the two blocks in question.
			Set<Vector2i> borderComp = VecGridUtil.getBorderRegionFromCoords2d(compLocalCoord);
			Set<Vector2i> corners = VecGridUtil.getCornerRegionCoords2d(borderComp);
			
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
			
			for (int coordIndex = 0; coordIndex < trueCoordsFirst.size(); coordIndex++) {
				Vector3i trueCoordFirst = trueCoordsFirst.get(coordIndex);
				AbstractNode node1 = new AbstractNode(trueCoordFirst);
				mainBlock.importantNodes.put(trueCoordFirst, node1);
				
				Vector3i trueCoordSecond = trueCoordsSecond.get(coordIndex);
				AbstractNode node2 = new AbstractNode(trueCoordSecond);
				otherBlock.importantNodes.put(trueCoordSecond, node2);
				
				node1.mirrorConnections.add(node2);
				node2.mirrorConnections.add(node1);
			}
		}
	}
	
	public AbstractNode addTempNode(LocalGridBlock block, Vector3i coords) {
		if (block.minBound != null) {
        	if (coords.x < block.minBound.x || 
        		coords.y < block.minBound.y ||
        		coords.z < block.minBound.z) { //If tile is not within the inclusive bounds
        		return null;
        	}
        }
        if (block.maxBound != null) {
        	if (coords.x > block.maxBound.x || 
        		coords.y > block.maxBound.y ||
        		coords.z > block.maxBound.z) {
        		return null;
        	}
        }
        if (block.importantNodes.containsKey(coords)) {
        	return block.importantNodes.get(coords);
        }
        AbstractNode node = new AbstractNode(coords);
        node.temporary = true;
        for (AbstractNode otherNode: block.importantNodes.values()) {
        	attemptConnectAbsNode(node, otherNode, null);
        	attemptConnectAbsNode(otherNode, node, null);
        }
        block.importantNodes.put(coords, node);
        return node;
	}
	
	public void removeTempNode(LocalGridBlock block, AbstractNode node) {
		if (node.temporary) {
			if (node.distToPathableNodes != null) {
				for (AbstractNode otherNode: node.distToPathableNodes.keySet()) {
					otherNode.distToPathableNodes.remove(node);
				}
				node.distToPathableNodes.clear();
			}
			block.importantNodes.remove(node);
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
		return VecGridUtil.getConnectedComponents(overlap, true);
	}
	
	private static class LocalGridBlock {
		public Vector3i minBound, maxBound;
		public Map<Vector3i, AbstractNode> importantNodes;
		
		public LocalGridBlock(Vector3i minB, Vector3i maxB) {
			this.minBound = minB;
			this.maxBound = maxB;
			importantNodes = new HashMap<>();
		}
	}
	
	private static class AbstractNode {
		public Vector3i coords;
		
		private Map<AbstractNode, ScoredPath> distToPathableNodes;
		
		public Set<AbstractNode> mirrorConnections;
		
		public boolean temporary = false;
		
		public AbstractNode(Vector3i coords) {
			this.coords = coords;
			this.mirrorConnections = new HashSet<>();
		}
		
		public ScoredPath getDist(AbstractNode otherNode) {
			if (distToPathableNodes == null) return null;
			return distToPathableNodes.get(otherNode);
		}
		
		public void addNode(AbstractNode node, ScoredPath path) {
			if (distToPathableNodes == null) {
				distToPathableNodes = new HashMap<>();
			}
			distToPathableNodes.put(node, path);
		}
		
		public String toString() {
			String string = "Coords: " + coords.toString() + ", distances: ";
			if (distToPathableNodes != null) {
				for (Entry<AbstractNode, ScoredPath> entry: distToPathableNodes.entrySet()) {
					string += "\n\t\t" + entry.getKey().coords + ", dist: " + entry.getValue().score;
					if (mirrorConnections.contains(entry.getKey())) {
						string += " M!";
					}
				}
			}
			return string;
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
	
	public static class ScoredAbstractPath {
    	public List<AbstractNode> path;
    	public double score;
    	public ScoredAbstractPath(List<AbstractNode> path, double score) {
    		this.path = path;
    		this.score = score;
    	}
    }
	
	public static void main(String[] args) {
		WorldCsvParser.init();
    	
    	Vector3i sizes = new Vector3i(50,50,50);
		
		long startTime = Calendar.getInstance().getTimeInMillis();
		
		LocalGrid activeLocalGrid = new LocalGridInstantiate(sizes, LocalGridBiome.defaultBiomeTest())
				.setupGrid(false);
		
		long endTime = Calendar.getInstance().getTimeInMillis();
		CustomLog.outPrintln("Init hierarchical pathfinder in " + (endTime - startTime) + "ms");
		
		Society testSociety = new Society("TestSociety", activeLocalGrid);
		testSociety.societyCenter = new Vector3i(10,10,10);
		
		HierarchicalPathfinder hPath = new HierarchicalPathfinder(activeLocalGrid);
		
		List<Human> people = new ArrayList<>();
		for (int j = 0; j < 1; j++) {
			int r = (int) (Math.random() * 99), c = (int) (Math.random() * 99);
			int h = activeLocalGrid.findHighestGroundHeight(r,c);
			
			Human human = new Human(testSociety, "Human" + j);
			people.add(human);
			activeLocalGrid.addHuman(human, new Vector3i(r,c,h));
		}
		testSociety.addHousehold(new Household(people));
		
		/*
		for (int r = 0; r < hPath.abstractBlocks.length; r++) {
			for (int c = 0; c < hPath.abstractBlocks[0].length; c++) {
				for (int h = 0; h < hPath.abstractBlocks[0][0].length; h++) {
					LocalGridBlock block = hPath.abstractBlocks[r][c][h];
					CustomLog.outPrintln("--------------------");
					CustomLog.outPrintln("Local Block Bounds: " + block.minBound + " -> " + block.maxBound);
					for (AbstractNode node: block.importantNodes.values()) {
						CustomLog.outPrintln("\t" + node.toString());
					}
				}
			}
		}
		*/
		
		Vector3i randStartCoords, randEndCoords;
		
		//randEndCoords = randStartCoords.getSum(new Vector3i(1,1,1));
		
		LocalGridBlock blockStart;
		LocalGridBlock blockEnd;
		//AbstractNode randStartNode = blockStart.importantNodes.values().iterator().next();
		//AbstractNode randEndNode = blockStart.importantNodes.values().iterator().next();
		CustomLog.outPrintln("Finding a viable path to test");
		
		ScoredPath path;
		Vector3i startVec, endVec;
		int numPathsTested = 0;
		while (numPathsTested < 10) {
			while (true) {
				randStartCoords = new Vector3i(
						(int) (Math.random() * hPath.abstractBlocks.length),
						(int) (Math.random() * hPath.abstractBlocks[0].length),
						(int) (Math.random() * hPath.abstractBlocks[0][0].length)
						);
				randEndCoords = new Vector3i(
						(int) (Math.random() * hPath.abstractBlocks.length),
						(int) (Math.random() * hPath.abstractBlocks[0].length),
						(int) (Math.random() * hPath.abstractBlocks[0][0].length)
						);
				
				blockStart = hPath.abstractBlocks[randStartCoords.x][randStartCoords.y][randStartCoords.z];
				blockEnd = hPath.abstractBlocks[randEndCoords.x][randEndCoords.y][randEndCoords.z];
				startVec = VecGridUtil.getRandVecInBounds(blockStart.minBound, blockStart.maxBound);
				endVec = VecGridUtil.getRandVecInBounds(blockEnd.minBound, blockEnd.maxBound);
				if (activeLocalGrid.tileIsAccessible(startVec) && activeLocalGrid.tileIsAccessible(endVec))
					if (activeLocalGrid.getTile(startVec).exposedToAir && activeLocalGrid.getTile(endVec).exposedToAir)
						break;
			}
			
			startTime = Calendar.getInstance().getTimeInMillis();
			
			path = hPath.findPath(null, startVec, endVec);
			
			endTime = Calendar.getInstance().getTimeInMillis();
			CustomLog.outPrintln("Completed trials in " + (endTime - startTime) + "ms");
			
			if (path != null && path.path != null && path.path.size() > 0) {
				CustomLog.outPrintln("Pathing: " + startVec + ", " + endVec + " ####################################");
				
				numPathsTested++;
				
				CustomLog.outPrintln("Hierarchical Path: ");
				CustomLog.outPrintln(path.path);
				CustomLog.outPrintln("Score (high means longer): " + path.score);
				
				List<Vector3i> vecs = new ArrayList<>();
				vecs.add(startVec); vecs.add(endVec);
				Pair<Vector3i> tempBounds = VecGridUtil.findCoordBounds(
						vecs
					);
				
				CustomLog.outPrintln("Regular Path: ");
				Pathfinder regPather = new Pathfinder(activeLocalGrid);
				ScoredPath oldStylePath = regPather.findPath(people.get(0), 
						activeLocalGrid.getTile(startVec), activeLocalGrid.getTile(endVec), 
						tempBounds.first, tempBounds.second);
				CustomLog.outPrintln(oldStylePath.path);
				CustomLog.outPrintln("Score (high means longer): " + oldStylePath.score);
			}
			else {
				continue;
			}
		}
	}
	
}
