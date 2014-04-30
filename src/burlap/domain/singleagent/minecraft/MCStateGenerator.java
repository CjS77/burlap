package burlap.domain.singleagent.minecraft;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.List;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

/**
 * @author dabel, gabrielbm
 * Reads in an ascii map of a 10x10 minecraft map and initializes a minecraft state.
 * Here is an example map file:
 * g
 * +++++++++ 
 * 
 * a
 * This will create a goal at (0,0,1), a wall from (0,1,2) to (8,1,2), and an agent
 * at (0,3,2).
 */
public class MCStateGenerator {
	
	private String fpath;
	
//	Symbols for parsing file
	private static final char gSym = 'g';
	private static final char bAddSym = '+';
	private static final char aSym = 'a';
	private static final char bRmSym = '-';
	private static final char dummySym = '.';
	private static final char wallSym = '=';
	private static final char doorSym = 'd';
	private static final char goldOreSym = '*';
	private static final char furnaceSym = 'o';
	private static final char lavaSym = 'V';
	private static final char twoBlockSym = '^';
	private int numRows;
	private int numCols;
	private int numBlocks;
	private int nTrenches = 0; // For use in random map generation
	
	
	public MCStateGenerator() {
		// TODO Auto-generated constructor stub
		// Convert relative path to absolute.
		String root = System.getProperty("user.dir");
		String abspath = root + "/maps/random/";
		
		// Generate a map based on the given LGD
		
		String[] map = generateRandomMap();

		Random r = new Random();
		try {
			abspath += r.nextInt(1000) + ".map";
			File f = new File(abspath);
			if (!f.exists()) {
				f.createNewFile();
				System.out.println("Created file");
			} else {
				System.out.println("File exists");
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsoluteFile()));
			
			bw.write("BN=" + nTrenches + "\n");
			for(int i = 0;i < map.length; i++) {
				for(int j = 0; j < map[i].length() - 1; j++) {
					bw.write(map[i].charAt(j) + " ");
				}
				bw.write(map[i].charAt(map[i].length() - 1) + "\n");
				bw.flush();
			}
			bw.close();
			
		}
		catch(IOException ex) {
			System.out.println("Inside catch");
			ex.printStackTrace();
			return;
		}

		// Write map to file, ProcessHeader()
		this.fpath = abspath;
		processHeader();
		
	}
	
	/**
	 * @param path the file path for the map file.
	 */
	public MCStateGenerator(String path) {
		// TODO Auto-generated constructor stub
		// Convert relative path to absolute.
		String root = System.getProperty("user.dir");
		String abspath = root + "/maps/" + path;
		this.fpath = abspath;
		processHeader();
	}
	
	public String[] generateRandomMap() {
		
		// Pick dimensions
		Random r = new Random();
		int n = r.nextInt(3) + 4;  // Generates a random number between 4 and 6
		
		String[] map = generateEmptyGrid(n);
		
		String trench = "";
		for (int i = 0; i < n; i++) {
			trench += bRmSym;
		}
		

		int maxTrenches = n / 2;
		double probOfTrench = 0.2;
		for (int i = 0; i < n; i++){
			// Flip a weighted coin for adding a trench or not
			if (r.nextDouble() < probOfTrench && nTrenches <= maxTrenches) {
				nTrenches++;
				// Flip a coin for orientation
				if (r.nextBoolean()) {
					// Horizontal
					map[i] = trench;
				}
				else {
					// Vertical
					for (int j = 0; j < n; j++) {
						map[j] = map[j].substring(0, i) + bRmSym + map[j].substring(i + 1);
					}
				}
			}

		}
		
		addRandomAgent(map);
		addRandomGoal(map);

		return map;		
	}
	
	private String[] generateEmptyGrid(int n) {
		String[] map = new String[n];
		
		String row = "";
		for (int i = 0; i < n; i++) {
			row += dummySym;
		}
		
		for (int i = 0; i < n; i++) {
			map[i] = row;
		}
		
		return map;
	}
	
	private void addRandomAgent(String[] map) {
		addRandomSym(map, aSym);
	}
	
	private void addRandomGoal(String[] map) {
		addRandomSym(map, gSym);
	}
	
	private void addRandomSym(String[] map, char sym) {
		Random r = new Random();
		while (true) {
			int x = r.nextInt(map.length);
			int y = r.nextInt(map.length);
			
			if (map[x].charAt(y) == dummySym) {
				map[x] = map[x].substring(0, y) + sym + map[x].substring(y + 1);
				break;
			}
		}
		
	}
	public int[] getDimensions() {
		try {
			Scanner scnr = new Scanner(new File(this.fpath));
			scnr.nextLine(); // Skip over header
			while (scnr.hasNextLine()) {
				String nextLine = scnr.nextLine();
				if (nextLine.length() > 0) {
					this.numRows++;
					this.numCols = nextLine.replace(" ", "").length() - 1;
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		this.numRows--;
		return new int[]{this.numCols, this.numRows};
	}
	
	private void processHeader() {
		// Sets classwide variables from header
		try {
			Scanner scnr = new Scanner(new File(this.fpath));
			String nextLine = scnr.nextLine(); // Skip over header
			String[] header = nextLine.split(",");
			
			for (String s : header) {
				String[] item = s.split("=");
				
				// ADD HEADER INFONS HERE
				if (item[0].equals("BN")) {
					numBlocks = Integer.parseInt(item[1]);
				}
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * This is the main method for the MCStateGenerator class.
	 * A new state is created, and we create an empty 10x10 floor.
	 * Next, the map file is read and adjustments are made to the empty 10x10 floor
	 * as necessary.
	 * @param d the uninitialized domain.
	 * @return the initialized State object.
	 */
	public State getCleanState(Domain d) {

		State s = new State();
		int nrow = 0;
		
		try {
			Scanner scnr = new Scanner(new File(this.fpath));
			
			scnr.nextLine(); // Skip over header
			
			while (scnr.hasNextLine()) {
				processRow(s, d, scnr.nextLine(), nrow);
				nrow++;
			}
			scnr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return s;
				
	}
	


	/**
	 * Here we read each ascii character in the row and make adjustments to the
	 * 10x10 empty state.
	 * 
	 * Floor is at z = 0. Everything else is added to z = 1. When a trench is read
	 * the block at z = 0 is removed.
	 * 
	 * @param s the state we are building
	 * @param d the uninitialized domain
	 * @param row the current map row in ascii format
	 * @param nrow the row number
	 */
	public void processRow(State s, Domain d, String row, int nrow) {
		char ch;
		int ncol = 0;
		row = row.replace(" ", "");
		
		
		while (ncol < row.length()) {

			ch = row.charAt(ncol);
			
			if (ch != ' ' && ch != '*') {
				// Floor placement
				addIndBlock(s, d, ncol, nrow, 0);
				addBlock(s, d, ncol, nrow, 1);
			}
			
			switch (ch) {
			case twoBlockSym:
				addBlock(s, d, ncol, nrow, 3);
			case bAddSym:
				addBlock(s, d, ncol, nrow, 2);
				ncol++;
				break;
			case aSym:
				addAgent(s, d, ncol, nrow, 2, numBlocks);
				ncol++;
				break;
			case gSym:
				addGoal(s, d, ncol, nrow, 2);
				ncol++;
				break;
			case bRmSym:
				removeBlock(s, d, ncol, nrow, 1);
				ncol++;
				break;
			case dummySym:
				ncol++;
				break;
			case wallSym:
				addIndBlock(s, d, ncol, nrow, 2);
				ncol++;
				break;
			case doorSym:
				addDoor(s, d, ncol, nrow, 2);
				addBlock(s, d, ncol, nrow, 3); // Add a block above doors so agent can't jump over
				ncol++;
				break;
			case goldOreSym:
				addIndBlock(s, d, ncol, nrow, 0);
				addGoldOre(s, d, ncol, nrow, 1);
				ncol++;
				break;
			case furnaceSym:
				addFurnace(s, d, ncol, nrow, 2);
				ncol++;
				break;
			case lavaSym:
				addLava(s, d, ncol, nrow, 1);
				ncol++;
			default:
				continue;
			}
		}
	}
	
	private static void addIndBlock(State s, Domain d, int x, int y, int z) {
		ObjectInstance wall = new ObjectInstance(d.getObjectClass("block"), "block"+x+y+z);
		wall.setValue("x", x);
		wall.setValue("y", y);
		wall.setValue("z", z);
		wall.setValue("isGoldOre", 0); //
		wall.setValue("attDestroyable", 0); // Walls cannot be destroyed
		s.addObject(wall);
	}
	
	private static void addDoor(State s, Domain d, int x, int y, int z) {
		ObjectInstance door = new ObjectInstance(d.getObjectClass("door"), "door"+x+y+z);
		door.setValue("x", x);
		door.setValue("y", y);
		door.setValue("z", z);
		door.setValue("doorOpen", 0); // door is closed at first.
		s.addObject(door);
	}
	
	private static void addGoldOre(State s, Domain d, int x, int y, int z) {
		ObjectInstance goldOre = new ObjectInstance(d.getObjectClass("block"), "block"+x+y+z);
		goldOre.setValue("x", x);
		goldOre.setValue("y", y);
		goldOre.setValue("z", z);
		goldOre.setValue("isGoldOre", 1); // door is closed at first.
		goldOre.setValue("attDestroyable", 1); // By default blocks can be destroyed
		s.addObject(goldOre);
	}
	
	private static void addFurnace(State s, Domain d, int x, int y, int z) {
		ObjectInstance furnace = new ObjectInstance(d.getObjectClass("furnace"), "furnace"+x+y+z);
		furnace.setValue("x", x);
		furnace.setValue("y", y);
		furnace.setValue("z", z);
		s.addObject(furnace);
	}
	
	private static void addBlock(State s, Domain d, int x, int y, int z) {
		ObjectInstance block = new ObjectInstance(d.getObjectClass("block"), "block"+x+y+z);
		block.setValue("x", x);
		block.setValue("y", y);
		block.setValue("z", z);
		block.setValue("isGoldOre", 0); //
		block.setValue("attDestroyable", 1); // By default blocks can be destroyed
		s.addObject(block);
	}
	
	private static void removeBlock(State s, Domain d, int x, int y, int z) {
		ObjectInstance block = s.getObject("block" + Integer.toString(x) + Integer.toString(y) + Integer.toString(z));
		s.removeObject(block);
	}
	
	private static void addAgent(State s, Domain d, int x, int y, int z, int agentNumBlocks) {
		ObjectInstance agent = new ObjectInstance(d.getObjectClass("agent"), "agent0");
		agent.setValue("bNum", agentNumBlocks);  // Expliticly set the number of blocks agent can carry to 1
		agent.setValue("agentHasGoldOre", 0);
		agent.setValue("agentHasGoldBlock", 0);
		addObject(agent, s, d, x, y, z);
	}

	private static void addGoal(State s, Domain d, int x, int y, int z) {
		ObjectInstance goal = new ObjectInstance(d.getObjectClass("goal"), "goal0");
		addObject(goal, s, d, x, y, z);
	}
	
	private static void addLava(State s, Domain d, int x, int y, int z) {
		ObjectInstance lava = new ObjectInstance(d.getObjectClass("lava"), "lava"+x+y+z);
		lava.setValue("x", x);
		lava.setValue("y", y);
		lava.setValue("z", z);
		s.addObject(lava);
	}
	
	private static void addObject(ObjectInstance obj, State s, Domain d, int x, int y, int z) {
		obj.setValue("x", x);
		obj.setValue("y", y);
		obj.setValue("z", z);
		s.addObject(obj);
	}
	

	public static void main(String[] args) {
		MinecraftDomain mcdg = new MinecraftDomain();
		Domain domain = mcdg.generateDomain();
		
		MCStateGenerator mcsg = new MCStateGenerator();
	}
	
	public int getMaxX() {
		return this.numCols;
	}
	public int getMaxY() {
		return this.numRows;
	}

	public int getBNum() {
		return numBlocks;
	}
	

}
