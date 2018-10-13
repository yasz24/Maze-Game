# Maze-Game
This project is the maze game. 
The objective is to create a new random maze every time you run the program. 
The player can try to solve the game manually or use a search algorithm to solve it for them.
When the player or the algorithm reaches the last vertex on the bottom right corner of the maze, the world ends. 
While the world has not ended, the player could press ”r” to reset the world state and create a new maze.
********
Arrow keys = player movement when manually solving the maze:
* “r” = generate a new random maze and moves the player back to the start
* “d” = implements a depth first search to find the path for the current maze; does not generate a new maze
* “b” = implements a breadth first search to find the path for the current maze; does not generate a new maze
********
Some other cool features in this game are:
1. Keeping score, which in this case is counting the number of wrong moves. If you make no wrong moves, the score is -1
2. Tearing down the walls to create the maze dynamically and animate its construction.
3. Keeping time from when the maze was constructed to when the maze was solved and this works for the player solving 
it as well as the algorithm.
