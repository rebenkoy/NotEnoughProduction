package com.bigbass.nep.gui.nodes;

import java.util.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.bigbass.nep.gui.Path;
import com.bigbass.nep.gui.borders.BorderedTable;
import com.bigbass.nep.recipes.RecipeManager;
import com.bigbass.nep.recipes.elements.Pile;
import com.bigbass.nep.recipes.processing.Recipe;
import com.bigbass.nep.skins.SkinManager;

/**
 * Represents a production node, most commonly a machine or a crafting table, with a particular recipe.
 */
public class Node {
	public final float width = 240;

	public enum Tier {
		ULV(1), LV(2), MV(3), HV(4), EV(5),
		IV(6), LUV(7), ZPM(8), UV(9), UHV(10), UEV(11);
		
		private int tierNum;
		
		private Tier(int t){
			this.tierNum = t;
		}
		
		public int getEUt(){
			return (int) Math.pow(2, (2 * tierNum) + 1);
		}
		
		public int compare(Tier other){
			if(tierNum > other.tierNum){
				return 1;
			} else if(tierNum < other.tierNum){
				return -1;
			} else {
				return 0;
			}
		}
		
		public int getTierNum(){
			return tierNum;
		}
		
		public static Tier getTierFromEUt(int eut){
			for(int i = 0; i < values().length; i++){
				Tier t = values()[i];
				if(eut < t.getEUt()){
					return t;
				}
			}
			
			return null;
		}
		
		public static Tier getTierFromNum(int num){
			for(int i = 0; i < values().length; i++){
				Tier t = values()[i];
				if(num == t.tierNum){
					return t;
				}
			}
			
			return null;
		}
	}
	
	private BorderedTable table;
	private Recipe recipe;
	protected Tier override;
	
	public Vector2 pos;
	
	private boolean shouldRemove;

	public UUID uuid;

	private int recipeHash;

	public Map<String, List<Path>> inputs;
	public Map<String, Path> outputs;

	private Map<String, Table> inputTables;
	private Map<String, Table> outputTables;

	public Node(float x, float y){
		this(x, y, null);
	}

	public Node(float x, float y, int recipeHash) {
		this(x, y, recipeHash, null);
	}

	public Node(float x, float y, int recipeHash, Tier override) {
		this(x, y, recipeHash, override, UUID.randomUUID());
	}

	private Node(float x, float y, int recipeHash, Tier override, UUID uuid) {
		this.uuid = uuid;
		this.pos = new Vector2(x, y);
		this.recipeHash = recipeHash;
		this.override = override;
		this.inputs = new HashMap<>();
		this.outputs = new HashMap<>();
		this.inputTables = new HashMap<>();
		this.outputTables = new HashMap<>();
		this.table = new BorderedTable(SkinManager.getSkin("fonts/droid-sans-mono.ttf", 10)); // font doesn't really matter here, but skin necessary for other stuff
		this.refreshRecipe(RecipeManager.getInst());
	}

	public Node(float x, float y, Recipe recipe){
		this(x, y, recipe, null);
	}

	public Node(float x, float y, Recipe recipe, Tier override){
		System.out.println("DEPRECATED Node Constructor");
		this.inputs = new HashMap<>();
		this.outputs = new HashMap<>();
		this.inputTables = new HashMap<>();
		this.outputTables = new HashMap<>();
		this.uuid = UUID.randomUUID();
		pos = new Vector2(x, y);
		
		table = new BorderedTable(SkinManager.getSkin("fonts/droid-sans-mono.ttf", 10)); // font doesn't really matter here, but skin necessary for other stuff
		
		this.override = override;
		
		shouldRemove = false;
		
		if(recipe != null){
			this.recipeHash = recipe.hashCode();
			refresh(recipe);
		} else {
			refresh();
		}
	}
	
	public void refresh(){
		NodeTableBuilder.build(this, table);
		table.setPosition(pos.x, pos.y);
	}
	
	public void refresh(Recipe recipe){
		this.recipe = recipe;
		for (Pile pile : recipe.inputs) {
			this.inputs.put(pile.element.name(), new LinkedList<>());
		}
		refresh();
	}
	
	public Actor getActor(){
		return table;
	}
	
	public Recipe getRecipe(){
		return recipe;
	}

	public void setOverride(Tier tier){
		override = tier;
		this.refresh();
	}
	public Tier getOverride(){
		return override;
	}
	
	public void setForRemoval(){
		shouldRemove = true;
	}
	public boolean shouldRemove(){
		return shouldRemove;
	}
	
	public JsonObject toJson(){
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("uuid", this.uuid.toString());

		builder.add("x", pos.x);
		builder.add("y", pos.y);
		
		if(override != null){
			builder.add("override", override.tierNum);
		}
		
		builder.add("recipeHash", (recipe == null ? -1 : recipe.hashCode()));
		
		return builder.build();
	}

	public static Node fromJson(JsonObject jsonNode) {
		final UUID uuid = UUID.fromString(jsonNode.getJsonString("uuid").getString());
		final float x = (float) jsonNode.getJsonNumber("x").doubleValue();
		final float y = (float) jsonNode.getJsonNumber("y").doubleValue();
		final int overrideNum = jsonNode.getInt("override", -1);
		Tier override = null;
		if(overrideNum != -1){
			override = Tier.getTierFromNum(overrideNum);
		}
		final int recipeHash = jsonNode.getInt("recipeHash", -1);

		return new Node(
				x,
				y,
				recipeHash,
				override,
				uuid
		);
	}

	public void refreshRecipe(RecipeManager rm) {
		this.refresh(rm.findRecipe(this.recipeHash));
	}

	public Vector2 getConnectionPos(String name, boolean input) {
		float x, y;
		if (input) {
			Table tbl = this.inputTables.get(name);
			y = this.pos.y + tbl.getY() + tbl.getPrefHeight() / 2;
			x = this.pos.x;
		} else {
			Table tbl = this.outputTables.get(name);
			y = this.pos.y + tbl.getY() + tbl.getPrefHeight() / 2;
			x = this.pos.x + this.width;
		}
		return new Vector2(x, y);
	}


	public void addInputTable(String name, Table table) {
		this.inputTables.put(name, table);
	}

	public void addOutputTable(String name, Table table) {
		this.outputTables.put(name, table);
	}
}
