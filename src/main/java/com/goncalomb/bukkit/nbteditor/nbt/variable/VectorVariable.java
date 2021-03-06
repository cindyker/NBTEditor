/*
 * Copyright (C) 2013, 2014 - Gonçalo Baltazar <http://goncalomb.com>
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

package com.goncalomb.bukkit.nbteditor.nbt.variable;

import com.goncalomb.bukkit.bkglib.Lang;
import com.goncalomb.bukkit.bkglib.reflect.NBTTagCompound;
import com.goncalomb.bukkit.nbteditor.NBTEditor;

public final class VectorVariable extends NBTGenericVariable{

	public VectorVariable(String nbtKey) {
		super(nbtKey);
	}
	
	boolean set(NBTTagCompound data, String value) {
		String[] pieces = value.replace(',', '.').split("\\s+", 3);
		if (pieces.length == 3) {
			Object[] vector = new Object[3];
			try {
				vector[0] = Double.parseDouble(pieces[0]);
				vector[1] = Double.parseDouble(pieces[1]);
				vector[2] = Double.parseDouble(pieces[2]);
			} catch (NumberFormatException e) {
				return false;
			}
			data.setList(_nbtKey, vector);
			return true;
		}
		return false;
	}
	
	String get(NBTTagCompound data) {
		if (data.hasKey(_nbtKey)) {
			Object[] vector = data.getListAsArray(_nbtKey);
			return (Double) vector[0] + ";" + (Double) vector[1] + ";" + (Double) vector[2];
		}
		return null;
	}
	
	String getFormat() {
		return Lang._(NBTEditor.class, "variable.formats.vector");
	}
	
}
