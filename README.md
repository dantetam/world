# World Civilization
World civilization exhibition at immense scale in individuals and their actions to build the world

![The 2d rendering of a 3d tile world which simulates civilization](https://github.com/dantetam/world/blob/master/res/promo/world_civ_promo.png?raw=true)

This project builds a physical world, and people to populate it. These people have complicated ideas of utility, economic prosperity, and their abilities to form societies and work together towards common goals. These people do not share a collective hive mind, but work on the basis of utility and rationality in doing their jobs, societal tasks, and so on.

```
Run the following file with normal Java configs:
io.github.dantetam.render/GameLauncher.java
```

## Research/Algorithmic Accomplishments ## 
Reverse topological search for resource utility: directly assign utility to some items, back-propogate utility to other items in a relational item graph structure

2D space packing algorithm: greedy structuring of 2D shapes within a larger designated 2D space. Extend beyond polyominos and optimal but slow algorithms.

Rectangle space optimization: maximal/perfect size rectangle search, 2d grid set cover

Pathfinding: hierarchical pathfinding (HPA*), extended to 3D and corners of 3D perimeters (maximal windows); rectangular symmetry reduction, also extended into 3D

Procedural Generation: 2D/3D Perlin and fractal noise, with custom parameters and thresholds, and surface topology operations. 

Multivariate approximation: combination of normal distributions to create clusters of resources, organisms, etc.

## Code/Game Accomplishments
Process, priority, and task deconstruction: create fine tasks as simple one-time commands from people's larger goals

Physical world: balanced use and availability of resources

Human thoughts and opinions: complicated use and change in values of ethos, opinions and flavors towards various topics

Combat system: use of tactics, bonuses, and calculations of hits compared to armor, damage, dodge, etc. Combat tactics for use in squads and armies vs other groups of people.

DNA, language, and culture spread. Language from Markov chain.

Free actions: events that can fire off, which humans can use to further the agency of their complicated AI: e.g., form households and societies, go to war, create new jobs, etc., all based on utility calculations.

## Citations

### For rectangular symmetry reduction pathfinding
D. Harabor, A. Botea, and P. Kilby. Path Symmetries in Uniform-cost Grid Maps. NICTA and The Australian National University. 2011. https://users.cecs.anu.edu.au/~dharabor/data/papers/harabor-botea-kilby-sara11.pdf

D. Harabor, A. Botea. Breaking Path Symmetries on 4-connected Grid Maps. NICTA and The Australian National University. 2010. https://users.cecs.anu.edu.au/~dharabor/data/papers/harabor-botea-aiide10.pdf

A. Botea, M. Muller, and J. Schaeffer. Near Optimal Hierarchical Path-Finding. Department of Computing Science, University of Alberta Edmonton. 2011. https://webdocs.cs.ualberta.ca/~mmueller/ps/hpastar.pdf

### For kd-tree, hierarchical data structures, and efficient KNN algs.
M. Adamsson, A. Vorkapic. A comparison study of Kd-tree, Vp-tree and Octree for storing neuronal morphology data with respect to performance. KTH Royal Institute of Technology. 2016. https://www.diva-portal.org/smash/get/diva2:928589/FULLTEXT01.pdf

Drost, B.H., Ilic, S. Almost constant-time 3D nearest-neighbor lookup using implicit octrees. Machine Vision and Applications 29, 299–311. 2018. https://doi.org/10.1007/s00138-017-0889-4

### For Voronoi influenced procedural generation, the actual Voronoi diagram alg.
A. Nocaj, U. Brandes. Computing Voronoi Treemaps: Faster, Simpler, and Resolution-independent. Department of Computer & Information Science, University of Konstanz. 2012. https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.259.5707&rep=rep1&type=pdf

### For an efficient, "anecdotal" color difference alg. that approximates non-Euclidean YUV/CIE approaches
T. Riemersma. Colour metric. CompuPhase. 2019. https://www.compuphase.com/cmetric.htm   

## Credits

"RPG Maker VX/Ace - Items" and "RPG Maker Tiles II" by [Ayene-chan](https://www.deviantart.com/ayene-chan)
"Shikashi's Fantasy Icons Pack" by [cheekyinkling](https://cheekyinkling.itch.io/shikashis-fantasy-icons-pack)