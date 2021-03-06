/*
 * Copyright (C) 2013 - Gonçalo Baltazar <http://goncalomb.com>
 *
 * This file is part of NBTEditor.
 *
 * NBTEditor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NBTEditor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NBTEditor.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.goncalomb.bukkit.nbteditor.commands;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.goncalomb.bukkit.bkglib.Lang;
import com.goncalomb.bukkit.bkglib.bkgcommand.BKgCommand;
import com.goncalomb.bukkit.bkglib.bkgcommand.BKgCommandException;
import com.goncalomb.bukkit.bkglib.bkgcommand.BKgCommand.TabComplete;
import com.goncalomb.bukkit.bkglib.namemaps.EntityTypeMap;
import com.goncalomb.bukkit.bkglib.utils.Utils;
import com.goncalomb.bukkit.bkglib.utils.UtilsMc;
import com.goncalomb.bukkit.nbteditor.NBTEditor;
import com.goncalomb.bukkit.nbteditor.bos.BookOfSouls;
import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.goncalomb.bukkit.nbteditor.nbt.FireworkNBT;
import com.goncalomb.bukkit.nbteditor.nbt.SpawnerEntityNBT;
import com.goncalomb.bukkit.nbteditor.nbt.SpawnerNBTWrapper;
import com.goncalomb.bukkit.nbteditor.nbt.variable.NBTVariable;

public class CommandNBTSpawner extends BKgCommand {
	
	public CommandNBTSpawner() {
		super("nbtspawner", "nbts");
	}
	
	private static SpawnerNBTWrapper getSpawner(Player player) throws BKgCommandException {
		Block block = UtilsMc.getTargetBlock(player, 5);
		if (block.getType() != Material.MOB_SPAWNER) {
			throw new BKgCommandException(Lang._(NBTEditor.class, "commands.nbtspawner.no-sight"));
		}
		return new SpawnerNBTWrapper(block);
	}
	
	private static int parseWeight(String[] args, int index) throws BKgCommandException {
		if (args.length > index) {
			int weight = Utils.parseInt(args[index], -1);
			if (weight < 1) {
				throw new BKgCommandException(Lang._(NBTEditor.class, "commands.nbtspawner.invalid-weight"));
			}
			return weight;
		}
		return 1;
	}
	
	private static int parseIndex(String str, List<SpawnerEntityNBT> entities) throws BKgCommandException {
		int index = Utils.parseInt(str, -1);
		if (index < 1) {
			throw new BKgCommandException(Lang._(NBTEditor.class, "invalid-index"));
		} else if (index > entities.size()) {
			throw new BKgCommandException(Lang._(NBTEditor.class, "commands.nbtspawner.entity-nop", index));
		}
		return index;
	}
	
	@Command(args = "info", type = CommandType.PLAYER_ONLY)
	public boolean infoCommand(CommandSender sender, String[] args) throws BKgCommandException {
		SpawnerNBTWrapper spawner = getSpawner((Player) sender);
		Location loc = spawner.getLocation();
		sender.sendMessage(ChatColor.GREEN + "Spawner Information (" + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + ")");
		sender.sendMessage("Current Entity: " + ChatColor.AQUA + EntityTypeMap.getName(spawner.getCurrentEntity()));
		for (SpawnerEntityNBT spawnerEntityNbt : spawner.getEntities()) {
			sender.sendMessage("   " + ChatColor.AQUA + EntityTypeMap.getName(spawnerEntityNbt.getEntityType()) + ", weight: " + spawnerEntityNbt.getWeight());
		}
		sender.sendMessage(ChatColor.GREEN + "Variables:");
		for (NBTVariable variable : spawner.getVariables()) {
			sender.sendMessage("   " + variable.getName() + ": " + ChatColor.AQUA + variable.getValue());
		}
		return true;
	}
	
	@Command(args = "var", type = CommandType.PLAYER_ONLY, maxargs = 2, usage = "<variable> <value>")
	public boolean varCommand(CommandSender sender, String[] args) throws BKgCommandException {
		SpawnerNBTWrapper spawner = getSpawner((Player) sender);
		if(args.length > 0) {
			NBTVariable variable = spawner.getVariable(args[0]);
			if (variable != null) {
				if(args.length == 2) {
					if (variable.setValue(args[1])) {
						spawner.save();
						sender.sendMessage(Lang._(NBTEditor.class, "variable.updated"));
						return true;
					} else {
						sender.sendMessage(Lang._(NBTEditor.class, "variable.invalid-format", args[0]));
					}
				}
				sender.sendMessage(ChatColor.YELLOW + variable.getFormat());
				return true;
			} else if(args.length <= 3) {
				sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.no-variable", args[0]));
			}
		}
		sender.sendMessage(Lang._(NBTEditor.class, "variables-prefix") + StringUtils.join(spawner.getVariables().getVarNames(), ", "));
		return false;
	}
	
	@TabComplete(args = "var")
	public List<String> var_tab(CommandSender sender, String[] args) {
		return (args.length == 1 ? Utils.getElementsWithPrefix(SpawnerNBTWrapper.variableNames(), args[0], true) : null);
	}
	
	@Command(args = "add", type = CommandType.PLAYER_ONLY, maxargs = 2, usage = "<entity> [weight]")
	public boolean addCommand(CommandSender sender, String[] args) throws BKgCommandException {
		if (args.length >= 1) {
			SpawnerNBTWrapper spawner = getSpawner((Player) sender);
			EntityType entityType = EntityTypeMap.getByName(args[0]);
			if (entityType != null && entityType.isAlive()) {
				if (entityType == EntityType.HORSE) {
					sender.sendMessage(ChatColor.YELLOW + "Due to a Minecraft bug, horse spawners are disabled!");
					sender.sendMessage(ChatColor.YELLOW + "Please go talk to Dinnerbone.");
					return true;
				}
				int weight = parseWeight(args, 1);
				spawner.addEntity(new SpawnerEntityNBT(entityType, weight));
				spawner.save();
				sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.entity-added"));
				return true;
			}
			sender.sendMessage(Lang._(NBTEditor.class, "invalid-entity"));
		}
		sender.sendMessage(Lang._(NBTEditor.class, "entities-prefix") + EntityTypeMap.getLivingNamesAsString());
		return false;
	}
	
	@TabComplete(args = "add")
	public List<String> add_tab(CommandSender sender, String[] args) {
		return (args.length == 1 ? Utils.getElementsWithPrefix(EntityTypeMap.getLivingNames(), args[0]) : null);
	}
	
	@Command(args = "additem", type = CommandType.PLAYER_ONLY, maxargs = 1, usage = "[weight]")
	public boolean additemCommand(CommandSender sender, String[] args) throws BKgCommandException {
		SpawnerNBTWrapper spawner = getSpawner((Player) sender);
		ItemStack item = ((Player) sender).getItemInHand();
		if (item.getType() == Material.MONSTER_EGG) {
			int weight = parseWeight(args, 0);
			EntityType entityType = EntityType.fromId(item.getDurability());
			if (EntityNBT.isValidType(entityType)) {
				spawner.addEntity(new SpawnerEntityNBT(EntityNBT.fromEntityType(entityType), weight));
				spawner.save();
				sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.entity-added"));
			} else {
				sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.invalid-egg"));
			}
		} else if (item.getType() == Material.FIREWORK) {
			int weight = parseWeight(args, 0);
			spawner.addEntity(new SpawnerEntityNBT(new FireworkNBT(item), weight));
			spawner.save();
			sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.firework-added"));
		} else {
			BookOfSouls bos = CommandBOS.getBos((Player) sender, true);
			if (bos != null) {
				int weight = parseWeight(args, 0);
				EntityNBT entityNbt = bos.getEntityNBT();
				// ----- 1.6 Bug ----- No Horse Spawners -----
				EntityNBT foo = entityNbt;
				do {
					if (foo.getEntityType() == EntityType.HORSE) {
						sender.sendMessage(ChatColor.YELLOW + "Due to a Minecraft bug, horse spawners are disabled!");
						sender.sendMessage(ChatColor.YELLOW + "Please go talk to Dinnerbone.");
						return true;
					}
				} while ((foo = foo.getRiding()) != null);
				// -------------------------------------------
				spawner.addEntity(new SpawnerEntityNBT(entityNbt, weight));
				spawner.save();
				sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.bos-added"));
			} else {
				sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.no-item"));
			}
		}
		return true;
	}
	
	@Command(args = "del", type = CommandType.PLAYER_ONLY, minargs = 1, usage = "<index>")
	public boolean delCommand(CommandSender sender, String[] args) throws BKgCommandException {
		SpawnerNBTWrapper spawner = getSpawner((Player) sender);
		int index = parseIndex(args[0], spawner.getEntities());
		spawner.removeEntity(index - 1);
		spawner.save();
		sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.entity-removed"));
		return true;
	}
	
	@Command(args = "setpos", type = CommandType.PLAYER_ONLY, minargs = 3, maxargs = 4, usage = "<x> <y> <z> [index]")
	public boolean setposCommand(CommandSender sender, String[] args) throws BKgCommandException {
		SpawnerNBTWrapper spawner = getSpawner((Player) sender);
		double x, y, z;
		try {
			x = Double.parseDouble(args[0]);
			y = Double.parseDouble(args[1]);
			z = Double.parseDouble(args[2]);
		} catch (NumberFormatException e) {
			sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.invalid-position"));
			return true;
		}
		if (args.length == 4) {
			List<SpawnerEntityNBT> entities = spawner.getEntities();
			int index = parseIndex(args[3], entities);
			EntityNBT entityNBT = entities.get(index - 1).getEntityNBT();
			entityNBT.setPos(x, y, z);
			sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.position-set", EntityTypeMap.getName(entityNBT.getEntityType())));
		} else {
			for (SpawnerEntityNBT spawnerEntity : spawner.getEntities()) {
				spawnerEntity.getEntityNBT().setPos(x, y, z);
			}
			sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.position-set-all"));
		}
		spawner.save();
		return true;
	}
	
	@Command(args = "clear", type = CommandType.PLAYER_ONLY)
	public boolean clearCommand(CommandSender sender, String[] args) throws BKgCommandException {
		SpawnerNBTWrapper spawner = getSpawner((Player) sender);
		spawner.clearEntities();
		spawner.save();
		sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.cleared"));
		return true;
	}
	
	@Command(args = "see", type = CommandType.PLAYER_ONLY)
	public boolean seeCommand(CommandSender sender, String[] args) throws BKgCommandException {
		Player player = (Player) sender;
		SpawnerNBTWrapper spawner = getSpawner(player);
		(new InventoryForSpawnerEntities(player, spawner)).openInventory(player, getOwner());
		return true;
	}
	
	@Command(args = "copy", type = CommandType.PLAYER_ONLY)
	public boolean copyCommand(CommandSender sender, String[] args) throws BKgCommandException {
		Player player = (Player) sender;
		SpawnerNBTWrapper clipboard = getSpawner(player);
		player.setMetadata("NBTEditor-spawner", new FixedMetadataValue(getOwner(), clipboard));
		sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.copy"));
		return true;
	}
	
	@Command(args = "paste", type = CommandType.PLAYER_ONLY)
	public boolean pasteCommand(CommandSender sender, String[] args) throws BKgCommandException {
		Player player = (Player) sender;
		if (player.hasMetadata("NBTEditor-spawner")) {
			SpawnerNBTWrapper spawner = getSpawner(player);
			spawner.cloneFrom((SpawnerNBTWrapper) player.getMetadata("NBTEditor-spawner").get(0).value());
			spawner.save();
			sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.paste"));
		} else {
			sender.sendMessage(Lang._(NBTEditor.class, "commands.nbtspawner.no-copy"));
		}
		return true;
	}
	
}