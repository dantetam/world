<groupName> refers to all items that have the group 'groupName'. This means to repeat a recipe or block for every item within the group.

The syntax for an item drop is:

trial1... ; trial2...; ...

where each trial is an independent space for item drops. Each item trial drops one type of item and can take the arguments:

Item name, minimum number to drop, maximum number to drop, probability of this drop
Name, min, max, prob

There are several shorthands for the four arguments above:

Name -> Name, 1, 1, 1
Name, Count -> Name, Count, Count, 1
Name, Min, Max -> Name, Min, Max, 1

 