import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/*
This project is the maze game. 
The objective is to create a new random maze every time you run the program. 
The player can try to solve the game manually or use a 
search algorithm to solve it for them.
When the player or the algorithm reaches the last vertex 
on the bottom right corner of the maze, the world ends. 
While the world has not ended, the player could press ”r” to reset 
the world state and create a new maze.
Arrow keys = player movement when manually solving the maze
“r” = generate a new random maze and moves the player back to the start
“d” = implements a depth first search to find the path for the current maze; does not generate a new maze
“b” = implements a breadth first search to find the path for the current maze; does not generate a new maze
Some extra features were also implement, that weren’t requirements for the assignment: 
1. Keeping score, which in this case is counting the number of wrong moves. If you make no wrong moves, the score is -1
2. Tearing down the walls to create the maze dynamically and animate its construction.
3. Keeping time from when the maze was constructed 
to when the maze was solved and this works for the player solving it as well as the algorithm.
*/

class Vertex {
  int name;
  int x;
  int y;
  ArrayList<Edge> outEdges;
  int reachTime;


  Vertex(int x, int y, int name) {
    this.x = x;
    this.y = y;
    this.name = name;
    this.outEdges = new ArrayList<Edge>();
    this.reachTime = 0;

  }

  //checks if this vertex is equal to that
  public boolean equals(Object other) {
    if (!(other instanceof Vertex)) {
      return false;
    }
    Vertex that = (Vertex) other;
    return this.name == that.name; 
  }

  // creates a hashcode for this vertex
  public int hashCode() {
    return this.name; 
  } 

  //draws all the visited nodes
  public WorldImage drawSeenVertex() {
    return new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE, OutlineMode.SOLID, 
        new Color(51, 153, 255));
  }

  //draws all the nodes on the Path
  public WorldImage drawPathVertex() {
    return new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE, OutlineMode.SOLID, 
        new Color(0, 0, 204));
  }
}

class Edge implements Comparable<Edge> {
  Vertex from;
  Vertex to;
  int weight;
  int reachTime;

  Edge(Vertex from, Vertex to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
    this.reachTime = 0;
  }

  //checks if this Edge is the same as that one
  public boolean equals(Object other) {
    if (!(other instanceof Edge)) {
      return false;
    }
    Edge that = (Edge) other;
    return (this.from.equals(that.from) && this.to.equals(that.to))
        || (this.to.equals(that.from) && this.from.equals(that.to));
  }       

  //draws all the edges in this graph
  public WorldImage drawEdge() {
    if (this.from.y == this.to.y) {
      //return new RectangleImage(Graph.CELL_SIZE - 1, Graph.CELL_SIZE -1, 
      //   OutlineMode.SOLID, Color.GRAY);
      return new LineImage(new Posn(0 , Graph.CELL_SIZE), Color.white);
    }
    else {
      return new LineImage(new Posn(Graph.CELL_SIZE, 0), Color.white);
    }
  }

  //compares two edges
  public int compareTo(Edge e2) {
    return this.weight - e2.weight;
  }
}


//represents the player class
class Player {
  int x;
  int y;
  ArrayList<Vertex> visited;

  Player(int x, int y) {
    this.x = x;
    this.y = y;
    this.visited = new ArrayList<Vertex>();
  }

  //moves player
  public void movePlayer(String ke) {
    if (ke.equals("up")) {
      this.y = y - 1;
    }
    if (ke.equals("down")) {
      this.y = y + 1;
    }
    if (ke.equals("left")) {
      this.x = x - 1;
    }
    if (ke.equals("right")) {
      this.x = x + 1;
    }
  }

  //draws the player
  public WorldImage drawPlayer() {
    return new CircleImage(Graph.CELL_SIZE / 4, OutlineMode.SOLID, Color.pink);
  }

}


class Graph extends World {
  ArrayList<ArrayList<Vertex>> board;
  ArrayList<Edge> allEdges;
  ArrayList<Edge> allEdges2;
  List<Edge> mst;
  List<Edge> mstCopy;
  ArrayList<Vertex> visited;
  ArrayList<Vertex> searchPath;

  Player player;
  int width;
  int height;

  int time;
  int seconds;
  int animationTime;

  //Defines an int constant
  static final int CELL_SIZE = 10;


  //constructor
  Graph(int width, int height) {
    this.width = width;
    this.height = height;
    this.player = new Player(0, 0);
    this.board = null;
    this.fixConnections();
    this.allEdges = this.sortEdges(this.getAllEdges());
    this.allEdges2 = this.sortEdges(this.getAllEdges());
    this.mst = this.kruskal();
    this.mstCopy = this.kruskal();
    this.removeMST();
    this.removeEdges();
    this.searchPath = new ArrayList<Vertex>(Arrays.asList(new Vertex(0, 0, 0)));
    this.visited = new ArrayList<Vertex>();
    this.time = 0;
    this.seconds = 0;
    this.animationTime = 0;
  }

  //secondary constructor for testing removeEdges
  //diff parameter exists to differentiate between constructors
  Graph(int width, int height, int diff) {
    this.width = width;
    this.height = height;
    this.player = new Player(0, 0);
    this.board = null;
    this.fixConnections();
    this.allEdges = this.sortEdges(this.getAllEdges());
    this.mst = this.kruskal();
    this.searchPath = new ArrayList<Vertex>(Arrays.asList(new Vertex(0, 0, 0)));
    this.visited = new ArrayList<Vertex>();
    this.time = 0;
  }


  //draws the world scene
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene((this.width) * CELL_SIZE, (this.height) * CELL_SIZE);
    scene.placeImageXY(new RectangleImage((this.width) * CELL_SIZE, (this.height) * CELL_SIZE,
        OutlineMode.SOLID, Color.GRAY), width * CELL_SIZE / 2, height * CELL_SIZE / 2);

    //sets the search path to the optimal path if the player solves the maze
    if (this.playerSolved()) {
      this.drawPath();
    }
    //draws all the vertices that are seen by the algorithm
    for (Vertex v : this.visited) {
      if (v.reachTime <= time) {
        scene.placeImageXY(v.drawSeenVertex(), v.x * CELL_SIZE + (CELL_SIZE / 2), 
            v.y * CELL_SIZE + (CELL_SIZE / 2)); 
      }
    }
    //draws the path that solves the maze
    if (time >= this.board.get(this.height - 1).get(this.width - 1).reachTime 
        || this.playerSolved()) {
      for (Vertex v : this.searchPath) {
        scene.placeImageXY(v.drawPathVertex(),v.x * CELL_SIZE + (CELL_SIZE / 2), 
            v.y * CELL_SIZE + (CELL_SIZE / 2));
      } 
    }
    for (Edge e : this.allEdges2) {
      if (e.from.y == e.to.y) {
        scene.placeImageXY(e.drawEdge(), e.from.x * CELL_SIZE + CELL_SIZE,
            e.from.y * CELL_SIZE + CELL_SIZE / 2);
      }
      else {
        scene.placeImageXY(e.drawEdge(), e.from.x * CELL_SIZE + CELL_SIZE / 2,
            e.from.y * CELL_SIZE + CELL_SIZE);
      }
    }
    scene.placeImageXY(this.player.drawPlayer(), 
        player.x * CELL_SIZE + (CELL_SIZE / 2), player.y * CELL_SIZE + (CELL_SIZE / 2));

    //extra feature: keeps a timer starting at the time when the maze has been constructed.
    if (this.mstCopy.size() == 0) {
      scene.placeImageXY(new TextImage("Time: " + (this.seconds) + "." + (this.time % 60), 
          15, Color.RED), 
          (this.width - (this.width / 4)) * CELL_SIZE, (this.height / 6) * CELL_SIZE); 
    }
    return scene;
  }

  //on tick method
  public void onTick() {
    time += 1;
    if ((time % 60) == 0 && (this.mstCopy.size() == 0)) {
      seconds ++;
    }
    //Bell : tears down the walls
    if (this.mstCopy.size() > 0) {
      Edge e = this.mstCopy.get(0);
      this.allEdges2.remove(e);
      this.mstCopy.remove(e);
    }
  }

  //draws optimal path when player solves the maze
  public void drawPath() {
    ArrayList<Vertex> temp = new ArrayList<Vertex>();
    for (Vertex v : this.visited) {
      temp.add(v);
    }
    this.searchPath = new ArrayList<Vertex>(Arrays.asList(new Vertex(0, 0, 0)));
    this.visited = new ArrayList<Vertex>();
    this.findPathDFS(this.board.get(0).get(0), this.board.get(this.height - 1).get(this.width - 1));
    this.visited = temp;
  }

  //updates the world on every tick
  public void onKeyEvent(String ke) {
    this.updatePath(ke);
    if (ke.equals("up")
        && (this.player.y != 0) && mst.contains(new Edge(board.get(player.y).get(player.x), 
            board.get(player.y - 1).get(player.x), 5))) {
      player.movePlayer(ke);
    }
    else if (ke.equals("left") 
        && (this.player.x != 0) && mst.contains(new Edge(board.get(player.y).get(player.x - 1), 
            board.get(player.y).get(player.x), 5))) {
      player.movePlayer(ke);
    }
    else if (ke.equals("right") && 
        (this.player.x != this.width - 1) && 
        mst.contains(new Edge(board.get(player.y).get(player.x + 1), 
            board.get(player.y).get(player.x), 5))) {
      player.movePlayer(ke);
    }
    else if (ke.equals("down") &&
        (this.player.y != this.height - 1) && 
        mst.contains(new Edge(board.get(player.y + 1).get(player.x), 
            board.get(player.y).get(player.x), 5))) {
      player.movePlayer(ke);
    }
    else if (ke.equals("b")) {
      seconds = 0;
      this.time = 0;
      this.searchPath = new ArrayList<Vertex>(Arrays.asList(new Vertex(0, 0, 0)));
      this.visited = new ArrayList<Vertex>();
      this.findPathBFS(this.board.get(0).get(0), 
          this.board.get(this.height - 1).get(this.width - 1));
    }
    else if (ke.equals("d")) {
      seconds = 0;
      this.time = 0;
      this.searchPath = new ArrayList<Vertex>(Arrays.asList(new Vertex(0, 0, 0)));
      this.visited = new ArrayList<Vertex>();
      this.findPathDFS(this.board.get(0).get(0), 
          this.board.get(this.height - 1).get(this.width - 1));
    }
    else if (ke.equals("r")) {
      seconds = 0;
      Graph g = new Graph(this.width, this.height);
      this.resetFields(g);
    }
  }

  //resets the fields on a key event
  public void resetFields(Graph g) {
    this.width = g.width;
    this.height = g.height;
    this.player = g.player;
    this.board = g.board;
    this.fixConnections();
    this.allEdges = this.sortEdges(this.getAllEdges());
    this.allEdges2 = this.sortEdges(this.getAllEdges());
    this.mst = this.kruskal();
    this.mstCopy = this.kruskal();
    this.removeMST();
    this.removeEdges();
    this.searchPath = new ArrayList<Vertex>(Arrays.asList(new Vertex(0, 0, 0)));
    this.visited = new ArrayList<Vertex>();
    this.time = 0;
  }

  //updates the path that the player has visited
  public void updatePath(String ke) {
    if (ke.equals("up") && (this.player.y != 0) &&
        mst.contains(new Edge(board.get(player.y).get(player.x), 
            board.get(player.y - 1).get(player.x), 5))) {
      visited.add(board.get(player.y - 1).get(player.x));
    }
    else if (ke.equals("left") && (this.player.x != 0) &&
        mst.contains(new Edge(board.get(player.y).get(player.x - 1), 
            board.get(player.y).get(player.x), 5))) {
      visited.add(board.get(player.y).get(player.x - 1));
    }
    else if (ke.equals("right") &&
        (this.player.x != this.width - 1) && 
        mst.contains(new Edge(board.get(player.y).get(player.x + 1), 
            board.get(player.y).get(player.x), 5))) {
      visited.add(board.get(player.y).get(player.x + 1));
    }
    else if (ke.equals("down") &&
        (this.player.y != this.height - 1) && 
        mst.contains(new Edge(board.get(player.y + 1).get(player.x), 
            board.get(player.y).get(player.x), 5))) {
      visited.add(board.get(player.y + 1).get(player.x));
    }
  }


  //world end method
  public WorldEnd worldEnds() {
    WorldScene finalScene = this.makeScene();
    if (this.playerSolved()) {
      return new WorldEnd(true, this.winScene());
    }
    else if (time == this.board.get(this.height - 1).get(this.width - 1).reachTime
        && this.searchPath.size() > 1) {
      return new WorldEnd(true, this.solveScene());
    }
    else {
      return new WorldEnd(false, finalScene);
    }
  }

  //check if the player has solved the maze
  public boolean playerSolved() {
    return this.player.x == this.board.get(this.height - 1).get(this.width - 1).x
        && this.player.y == this.board.get(this.height - 1).get(this.width - 1).y;
  }

  //checks if the player has almost solved the maze
  public boolean almostSolved() {
    return Math.abs(this.player.x - this.board.get(this.height - 1).get(this.width - 1).x) <= 1
        && Math.abs(this.player.y - this.board.get(this.height - 1).get(this.width - 1).y) <= 1;
  }

  //Image displayed when player wins
  public WorldScene winScene() {
    WorldScene scene = this.makeScene();
    scene.placeImageXY(new AboveImage(new TextImage("GAME OVER. YOU SOLVED THE MAZE!", 20, 
        new Color(51, 255, 255)), 
        //whistle: counts the number of wrong moves
        new TextImage("WRONG MOVES: " + (this.visited.size() - this.searchPath.size()), 
            20,  new Color(51, 255, 255))), (this.width / 2) * CELL_SIZE,
        (this.height / 2) * CELL_SIZE);
    return scene;
  }

  //Image displayed when player wins
  public WorldScene solveScene() {
    WorldScene scene = this.makeScene();
    scene.placeImageXY(new AboveImage(new TextImage("THE MAZE HAS BEEN SOLVED!", 20, 
        new Color(51, 255, 255)), 
        //whistle: counts the number of wrong moves
        new TextImage("WRONG MOVES: " + (this.visited.size() - this.searchPath.size()), 
            20,  new Color(51, 255, 255))), (this.width / 2) * CELL_SIZE,
        (this.height / 2) * CELL_SIZE);
    return scene;
  }


  //creates the entire maze of vertices
  public ArrayList<ArrayList<Vertex>> createVertices() {
    ArrayList<ArrayList<Vertex>> matrix = new ArrayList<ArrayList<Vertex>>();
    int multiple = 0;
    for (int i = 0; i < this.height; i += 1 ) {
      ArrayList<Vertex> rows = new  ArrayList<Vertex>();
      for (int j = 0; j < this.width; j += 1) {
        rows.add(j, new Vertex(j, i, multiple + j));
      }
      matrix.add(rows);
      multiple += this.width;
    }
    return matrix;
  }

  //fixes the edges for each vertex in this grid and creates edges whenever a neighboring
  //vertex is not itself
  public void fixConnections() {
    ArrayList<ArrayList<Vertex>> grid = this.createVertices();
    Random rand = new Random();
    Edge e;
    for (int i = 0; i < this.height; i += 1) {
      for (int j = 0; j < this.width; j += 1) {
        if ((j != width - 1)) {
          e = new Edge(grid.get(i).get(j), grid.get(i).get(j + 1), 
              rand.nextInt(this.height * this.width));
          grid.get(i).get(j).outEdges.add(e);
          grid.get(i).get(j + 1).outEdges.add(e);
        }
        if ((i != height - 1)) {
          e = new Edge(grid.get(i).get(j), grid.get(i + 1).get(j), 
              rand.nextInt(this.height * this.width));
          grid.get(i).get(j).outEdges.add(e);
          grid.get(i + 1).get(j).outEdges.add(e);
        }
      }
    }
    this.board = grid;
  }

  //gets all the edges that connect the vertices in the graph
  public ArrayList<Edge> getAllEdges() {
    ArrayList<Edge> seen = new ArrayList<Edge>();
    for (int i = 0; i < this.height; i += 1) {
      for (int j = 0; j < this.width; j += 1) {
        for (Edge e : this.board.get(i).get(j).outEdges) {
          if (!seen.contains(e)) {
            seen.add(e); 
          }
        }
      }
    }
    return seen;
  }

  //sorts all the edges in this graph by their weights
  public ArrayList<Edge> sortEdges(ArrayList<Edge> allEdges) {
    Collections.sort(allEdges);
    return allEdges;
  }


  //implements the Kruskal algorithm
  public List<Edge> kruskal() {
    HashMap<Vertex, Vertex> representatives = new HashMap<Vertex, Vertex>();
    List<Edge> edgesInTree = new ArrayList<Edge>();
    List<Edge> worklist = this.getAllEdges();
    Collections.sort(worklist);

    //initialize every node's representative to itself
    for (int i = 0; i < this.height; i += 1) {
      for (int j = 0; j < this.width; j += 1) {
        representatives.put(board.get(i).get(j), board.get(i).get(j)); 
      }
    }
    while (worklist.size() > 0) {
      Edge e = worklist.remove(0);

      if (find(representatives, e.from).equals(find(representatives, e.to))) {  
        //does nothing
      }
      else {
        edgesInTree.add(e);
        this.union(representatives, find(representatives, e.from), find(representatives, e.to));
      }
    }
    return edgesInTree;
  }

  //returns the representative for this vertex
  public Vertex find(HashMap<Vertex, Vertex> representative, Vertex v) {
    if (representative.get(v).equals(v)) {
      return representative.get(v);
    }
    else {
      return find(representative, representative.get(v));
    }
  }

  public void union(HashMap<Vertex, Vertex> representatives, Vertex v1, Vertex v2) {
    representatives.put(v2, v1);
  }


  //implements the breadth first search 
  public void findPathDFS(Vertex from, Vertex to) {
    this.findPath(from, to, new Stack<Vertex>());
  }

  //implements the depth first search
  public void findPathBFS(Vertex from, Vertex to) {
    this.findPath(from, to, new Queue<Vertex>());
  }


  //the generic find path method: helper to both findPathDFS and findPathBFS
  public void findPath(Vertex from, Vertex to, ICollection<Vertex> worklist) {
    int count = 0;
    HashMap<Vertex, Vertex> cameFromEdge = new HashMap<Vertex, Vertex>();
    worklist.add(from);
    cameFromEdge.put(from, from);

    while (worklist.size() > 0) {
      Vertex current = worklist.remove();

      if (visited.contains(current)) {
        // Discard node : Do nothing
      }
      else if (current.equals(to)) {
        while (!(current.equals(cameFromEdge.get(current)))) {
          searchPath.add(current);
          current = cameFromEdge.get(current);
        }
        return;
      }
      else {
        for (Edge e : current.outEdges) {
          if (current.equals(e.from) && !visited.contains(e.to)) {
            worklist.add(e.to);
            cameFromEdge.put(e.to, e.from);
            e.to.reachTime = count;
          }
          else if (current.equals(e.to) && !visited.contains(e.from)) {
            worklist.add(e.from);
            cameFromEdge.put(e.from, e.to);
            e.from.reachTime = count;
          }
        }
        visited.add(current);
      }
      count ++;
    }
  }

  //removes the minimum spanning tree from the list of all edges
  public void removeMST() {
    int count = 0;
    for (Edge me : this.mst) {
      allEdges.remove(me);
      me.reachTime = count;
      count += 1;
    }
    this.animationTime = count;
  }

  //removes all the edges that aren't in the MST from the from vertex
  //of the outEdges of each Edge
  public void removeEdges() {
    ArrayList<Edge> temp = new ArrayList<Edge>();
    for (Edge e : allEdges) {
      temp.add(e);
    }

    for (Edge e : temp) {
      e.from.outEdges.remove(e);
      e.to.outEdges.remove(e);
    }
  }
}



interface ICollection<T> {
  
  //removes things from the collection
  T remove();
  
  //adds things to the collection
  void add(T t);
  
  //gives the size of the collection
  int size();
}

class Stack<T> implements ICollection<T> {
  Deque<T> items;

  Stack() {
    this.items = new Deque<T>();
  }

  //Effect: remove from the stack
  //returns the item that is removed
  public T remove() {
    return this.items.removeFromHead();
  }

  //Effect: adds the given item to the stack
  public void add(T t) {
    this.items.addAtHead(t);
  }

  public int size() {
    return this.items.size();
  }
}

class Queue<T> implements ICollection<T> {
  Deque<T> items;

  Queue() {
    this.items = new Deque<T>();
  }

  //Effect: remove from the stack
  //returns the item that is removed
  public T remove() {
    return this.items.removeFromHead();
  }

  //Effect: adds the given item to the stack
  public void add(T t) {
    this.items.addAtTail(t);
  }

  public int size() {
    return this.items.size();
  }
}



//examples class
class ExamplesMaze {
  Vertex v1;
  Vertex v2;
  Vertex v3;
  Vertex v4;
  Vertex v5;
  Vertex v6;
  Vertex v7;
  Vertex v8;
  Vertex v9;
  Vertex v10;
  Vertex v11;
  Vertex v12;
  Vertex v13;
  Vertex v14;
  HashMap<Vertex, Vertex> rep;

  //random edges
  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;
  Edge e5;

  //edges for kruskals testing
  Edge e6;
  Edge e7;
  Edge e8;
  Edge e9;
  Edge e10;
  Edge e11;
  Edge e12;

  //edge list
  ArrayList<Edge> elist;
  //unordered edge list
  ArrayList<Edge> uoElist;
  //spanning tree for egraph
  ArrayList<Edge> spanningTree;
  Graph egraph;
  Graph graph;
  Graph graph1;
  Graph graph2;
  Graph graph3;
  Graph testGraph;

  // player
  Player p1;

  //reset method
  void reset() {
    //spare vertices
    v1 = new Vertex(4, 4, 10);
    v2 = new Vertex(8, 3, 15);
    v3 = new Vertex(10, 5, 20);
    v4 = new Vertex(1, 1, 1);
    v5 = new Vertex(4, 4, 10);
    //matrix level 1
    v6 = new Vertex(0, 0, 0);
    v7 = new Vertex(1, 0, 1);
    v8 = new Vertex(2, 0, 2);

    //matrix level 2
    v9 = new Vertex(0, 1, 3);
    v10 = new Vertex(1, 1, 4);
    v11 = new Vertex(2, 1, 5);
    //matrix level 3
    v12 = new Vertex(0, 2, 6);
    v13 = new Vertex(1, 2, 7);
    v14 = new Vertex(2, 2, 8);
    //hash map for representatives of vertices
    rep = new HashMap<Vertex, Vertex>();
    rep.put(v6, v6);
    rep.put(v7, v7);
    rep.put(v8, v8);
    rep.put(v9, v9);
    rep.put(v10, v10);
    rep.put(v11, v11);
    rep.put(v12, v12);
    rep.put(v13, v13);
    rep.put(v14, v14);

    //random edges
    e1 = new Edge(v1, v2, 5);
    e2 = new Edge(v2, v3, 1);
    e3 = new Edge(v1, v3, 2);
    e4 = new Edge(v3, v4, 6);
    e5 = new Edge(v1, v5, 8);
    //edges for kruskals testing
    e6 = new Edge(v1, v2, 1);
    e7 = new Edge(v2, v3, 2);
    e8 = new Edge(v1, v6, 3);
    e9 = new Edge(v5, v6, 4);
    e10 = new Edge(v2, v5, 6);
    e11 = new Edge(v4, v5, 7);
    e12 = new Edge(v3, v4, 8);
    //edge list
    elist = new ArrayList<Edge>(Arrays.asList(e6, e7, e8, e9, e10, e11, e12));
    //unordered edge list
    uoElist = new ArrayList<Edge>(Arrays.asList(e7, e9, e6, e12, e8, e11, e10));
    //spanning tree for egraph
    spanningTree = new ArrayList<Edge>(Arrays.asList(e6, e7, e8, e9, e11));
    //graph
    graph = new Graph(3, 3);
    graph1 = new Graph(5, 5);
    graph2 = new Graph(10, 10);
    graph3 = new Graph(50, 50);
    testGraph = new Graph(10, 10, 1);

    // player
    p1 = new Player(5, 5);
  }

  //testing createVertices
  void testCreateVertices(Tester t) {
    this.reset();
    ArrayList<Vertex> list1 = new ArrayList<Vertex>(Arrays.asList(v6, v7, v8));
    ArrayList<Vertex> list2 = new ArrayList<Vertex>(Arrays.asList(v9, v10, v11));
    ArrayList<Vertex> list3 = new ArrayList<Vertex>(Arrays.asList(v12, v13, v14));
    ArrayList<ArrayList<Vertex>> matrix = new ArrayList<ArrayList<Vertex>>(
        Arrays.asList(list1, list2, list3));

    for (int i = 0; i < 3; i += 1) {
      for (int j = 0; j < 3; j += 1) {
        t.checkExpect(graph.board.get(i).get(j).equals(matrix.get(i).get(j)), true);
      }
    }
  }

  //testing equal vertex method
  void testEqualVertex(Tester t) {
    this.reset();
    t.checkExpect(this.v1.equals(this.v2), false);
    t.checkExpect(this.v1.equals(this.v1), true);
    t.checkExpect(this.v1.equals(this.v5), true);
  }

  //testing equal edge method
  void testEqualEdge(Tester t) {
    this.reset();
    t.checkExpect(this.e1.equals(this.e2), false);
    t.checkExpect(this.e1.equals(this.e1), true);
  }

  //test hash code
  void testHashCode(Tester t) {
    this.reset();
    t.checkExpect(this.v3.hashCode() == this.v4.hashCode(), false);
    t.checkExpect(this.v1.hashCode() == this.v5.hashCode(), true);
    t.checkExpect(this.v1.hashCode() == this.v1.hashCode(), true);
  }

  //test draw edge
  void testDrawEdge(Tester t) {
    this.reset();
    t.checkExpect(this.e1.drawEdge(), new LineImage(new Posn(10, 0), Color.white));
    t.checkExpect(this.e5.drawEdge(), new LineImage(new Posn(0, 10), Color.white));
  }

  //test find
  void testFind(Tester t) {
    this.reset();
    t.checkExpect(this.graph.find(this.rep, v6), v6);
    t.checkExpect(this.graph.find(this.rep, v9), v9);
    t.checkExpect(this.graph.find(this.rep, v14), v14);
    t.checkFail(this.graph.find(this.rep, v13), v10);
  }

  //test union
  void testUnion(Tester t) {
    this.reset();
    t.checkExpect(this.graph.find(this.rep, v6), v6);
    this.graph.union(this.rep, v6, v10);
    t.checkExpect(this.graph.find(this.rep, v6), v6);
    t.checkExpect(this.graph.find(this.rep, v10), v6);
    t.checkExpect(this.graph.find(this.rep, v8), v8);
    this.graph.union(this.rep, v8, v13);
    t.checkExpect(this.graph.find(this.rep, v8), v8);
    t.checkExpect(this.graph.find(this.rep, v13), v8);

  }

  //test kruskals
  void testKruskals(Tester t) {
    this.reset();
    t.checkExpect((this.graph.kruskal().size() >= this.graph.allEdges.size()), true);
    t.checkExpect((this.graph1.kruskal().size() >= this.graph1.allEdges.size()), true);
    t.checkExpect((this.graph2.kruskal().size() >= this.graph2.allEdges.size()), true);
    t.checkExpect((this.graph3.kruskal().size() >= this.graph3.allEdges.size()), true);
  }

  //test sort edges
  void testSortEdges(Tester t) {
    this.reset();
    t.checkExpect(graph.sortEdges(this.uoElist), this.elist);
  }

  //test get all edges
  void testGetAllEdges(Tester t) {
    this.reset();
    t.checkExpect((this.graph.getAllEdges().size() != this.graph.allEdges.size()), true);
    t.checkExpect((this.graph1.getAllEdges().size() != this.graph1.allEdges.size()), true);
    t.checkExpect(this.graph1.getAllEdges().size() == this.graph2.getAllEdges().size(), false);
    t.checkExpect(this.graph2.getAllEdges().size() == this.graph1.getAllEdges().size(), false);
    t.checkExpect(this.graph3.getAllEdges().size() == this.graph2.getAllEdges().size(), false);
  }

  //void test remove MST
  void testRemoveMST(Tester t) {
    this.reset();
    List<Edge> list1 = graph3.kruskal();
    graph3.removeMST();
    for (Edge e : list1) {
      t.checkExpect(graph3.allEdges.contains(e), false);
    }
  }

  //testing fix connections
  void testFixConnections(Tester t) {
    this.reset();
    this.graph.fixConnections();
    //testing edge creations
    t.checkExpect(
        this.graph.board.get(0).get(0).outEdges.get(0).from.equals(this.graph.board.get(0).get(0)),
        true);
    t.checkExpect(
        this.graph.board.get(0).get(0).outEdges.get(0).to.equals(this.graph.board.get(0).get(1)),
        true);
  }

  //test compareTo
  void testCompareTo(Tester t) {
    this.reset();

    t.checkExpect(this.e1.compareTo(this.e2), 4);
    t.checkExpect(this.e3.compareTo(this.e4), -4);
  }

  // testing drawSeenVertex
  void testDrawSeenVertex(Tester t) {
    this.reset();
    t.checkExpect(this.v1.drawSeenVertex(),
        new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE, OutlineMode.SOLID,
            new Color(51, 153, 255)));
  }

  // testing drawPathVertex
  void testDrawPathVertex(Tester t) {
    this.reset();
    t.checkExpect(this.v1.drawPathVertex(), 
        new RectangleImage(Graph.CELL_SIZE, Graph.CELL_SIZE, OutlineMode.SOLID, 
            new Color(0, 0, 204)));
  }

  // test movePlayer
  void testMovePlayer(Tester t) {
    this.reset();
    t.checkExpect(this.p1.y, 5);
    this.p1.movePlayer("up");
    t.checkExpect(this.p1.y, 4);
    this.p1.movePlayer("down");
    t.checkExpect(this.p1.y, 5);
    this.p1.movePlayer("left");
    t.checkExpect(this.p1.x, 4);
    this.p1.movePlayer("right");
    t.checkExpect(this.p1.x, 5);
  }

  // test drawPlayer
  void testDrawPlayer(Tester t) {
    this.reset();
    t.checkExpect(this.p1.drawPlayer(), 
        new CircleImage(Graph.CELL_SIZE / 4, OutlineMode.SOLID, Color.pink));
  }

  // test onTick
  void testOnTick(Tester t) {
    this.reset();
    t.checkExpect(this.graph.time, 0);
    this.graph.onTick();
    t.checkExpect(this.graph.time, 1);
  }

  // test resetFields
  void testResetFields(Tester t) {
    this.reset();
    while (this.graph.time != 5) {
      this.graph.onTick();
    }
    t.checkExpect(this.graph.time, 5);
    this.graph.resetFields(this.graph);
    t.checkExpect(this.graph.time, 0);
  }

  // test on key event
  void testOnKeyEvent(Tester t) {
    this.reset();
    while (this.graph.time <= 5) {  
      this.graph.onTick();
    }
    this.graph.onKeyEvent("b");
    t.checkExpect(this.graph.time, 0);
    this.reset();
    while (this.graph.time <= 5) {
      this.graph.onTick();
    }
    this.graph.onKeyEvent("d");
    t.checkExpect(this.graph.time, 0);
    this.reset();
    while (this.graph.time <= 0) {
      this.graph.onTick();
    }
    this.graph.onKeyEvent("r"); 
    t.checkExpect(this.graph.time, 0);
    this.reset();
    this.graph1.player = new Player(2, 2);
    this.graph1.player.movePlayer("up");
    t.checkExpect(this.graph1.player.y, 1);
  }

  // test updatePath
  void testUpdatePath(Tester t) {
    this.reset();
    this.graph.player = new Player(2, 2);
    int originalVisited = this.graph.visited.size();
    this.graph.updatePath("up");
    this.graph.updatePath("down");
    this.graph.updatePath("left");
    this.graph.updatePath("right");
    t.checkExpect(this.graph.visited.size() > originalVisited, true);
  } 

  // test removeEdges
  void testRemoveEdges(Tester t) {
    this.reset();
    int temp = this.testGraph.allEdges.get(0).from.outEdges.size();
    this.testGraph.removeEdges();
    this.testGraph.removeMST();
    t.checkExpect(this.testGraph.allEdges.get(0).from.outEdges.size() < temp, true);
  }

  // test drawPath
  void testDrawPath(Tester t) {
    this.reset();
    int temp = this.graph.visited.size();
    this.graph.drawPath();
    t.checkExpect(this.graph.visited.size() <= temp, true);
  }

  // test playerSolved
  void testPlayerSolved(Tester t) {
    this.reset();
    this.graph.player = new Player(
        this.graph.board.get(graph.height - 1).get(graph.width - 1).x,
        this.graph.board.get(graph.height - 1).get(graph.width - 1).y);
    t.checkExpect(this.graph.playerSolved(), true);
  }

  // test almostSolved
  void testAlmostSolved(Tester t) {
    this.reset();
    this.graph.player = new Player(2, 2);
    t.checkExpect(this.graph.almostSolved(), true);
  }

  //test for outputting the world images
  void testBigBang(Tester t) {
    Graph graph = new Graph(50, 50);
    graph.bigBang(500, 500, 0.005);
  }

}
