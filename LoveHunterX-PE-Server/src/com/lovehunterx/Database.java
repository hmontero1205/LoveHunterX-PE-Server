package com.lovehunterx;
import java.sql.*;

public class Database {
	private static final String DATABASE = "jdbc:mysql://localhost:3306/lovehunterx?autoReconnect=true";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "lovehunterx";
	private Connection con;

	public Database() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(DATABASE, USERNAME, PASSWORD);
			//con = DriverManager.getConnection("jdbc:mysql://localhost:3306/hans", "root", "");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public boolean authenticate(String u, String p) {
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE name = ? AND pass = ?");
			ps.setString(1, u);
			ps.setString(2, p);
			ResultSet rs = ps.executeQuery();
			return rs.next();
		} catch (SQLException e) {
			System.out.println(e);
		}
		return false;
	}

	public boolean register(String u, String p) {
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE name = ?");
			ps.setString(1, u);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return false;
			else {
				PreparedStatement insStatement = con.prepareStatement("INSERT INTO users (name, pass, sprite_number) VALUES (?, ?, ?)");
				insStatement.setString(1, u);
				insStatement.setString(2, p);
				insStatement.setInt(3, 0);
				insStatement.executeUpdate();

				PreparedStatement insStatement2 = con.prepareStatement("INSERT INTO inventories (user, type, amount) VALUES (?, ?, ?)");
				insStatement2.setString(1, u);
				insStatement2.setString(2, "Love Sofa");
				insStatement2.setInt(3, 3);
				insStatement2.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void changeSprite(String user, int sprite) {
		try {
			PreparedStatement ps = con.prepareStatement("UPDATE users SET sprite_number = ? WHERE name = ?");
			ps.setInt(1, sprite);
			ps.setString(2, user);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String setFurniture(String x, String y, String uid, String type, String user) {
		try {
			PreparedStatement checkStatement = con.prepareStatement("SELECT * FROM furnitures WHERE uid = ?");
			checkStatement.setInt(1, Integer.valueOf(uid));
			ResultSet rs = checkStatement.executeQuery();
			if (rs.next()){
				PreparedStatement updateStatement = con.prepareStatement("UPDATE furnitures SET x = ?, y = ? WHERE uid = ?");
				updateStatement.setFloat(1, Float.valueOf(x));
				updateStatement.setFloat(2, Float.valueOf(y));
				updateStatement.setFloat(3, Integer.valueOf(uid));
				updateStatement.executeUpdate();
				
				return uid;
			} else {
				PreparedStatement checkStatement2 = con.prepareStatement("SELECT amount FROM inventories WHERE user = ? AND type = ?");
				checkStatement2.setString(1, user);
				checkStatement2.setString(2, type);
				ResultSet rs2 = checkStatement2.executeQuery();

				Integer currentAmount;
				if (rs2.next() && (currentAmount = rs2.getInt("amount")) > 0) {
					PreparedStatement updateStatement = con.prepareStatement("UPDATE inventories SET amount = ? WHERE user = ? AND type = ?");
					updateStatement.setInt(1, currentAmount - 1);
					updateStatement.setString(2, user);
					updateStatement.setString(3, type);
					updateStatement.executeUpdate();
					
					PreparedStatement insertStatement = con.prepareStatement("INSERT INTO furnitures (room, type, x, y) VALUES (?, ?, ?, ?)");
					insertStatement.setString(1, user);
					insertStatement.setString(2, type);
					insertStatement.setFloat(3, Float.valueOf(x));
					insertStatement.setFloat(4, Float.valueOf(y));
					insertStatement.executeUpdate();
					
					PreparedStatement lastRowStatement = con.prepareStatement("SELECT * FROM furnitures ORDER BY uid DESC LIMIT 1");
					ResultSet rs3 = lastRowStatement.executeQuery();

					if (rs3.next()) {
						int createdUID = rs3.getInt("uid");
						return Integer.toString(createdUID);
					}
				}
				
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public ResultSet getInventory(String user) {
		try {
			PreparedStatement getStatement = con.prepareStatement("SELECT * FROM inventories WHERE user = ?");
			getStatement.setString(1, user);
			return getStatement.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public ResultSet getFurniture(String room) {
		try {
			PreparedStatement getStatement = con.prepareStatement("SELECT * FROM furnitures WHERE room = ?");
			getStatement.setString(1, room);
			return getStatement.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void removeFurniture(String uid, String type, String name) {
		try {
			PreparedStatement removeStatement = con.prepareStatement("DELETE FROM furnitures WHERE uid = ?");
			removeStatement.setInt(1, Integer.parseInt(uid));
			removeStatement.executeUpdate();
			
			PreparedStatement checkStatement2 = con.prepareStatement("SELECT amount FROM inventories WHERE user = ? AND type = ?");
			checkStatement2.setString(1, name);
			checkStatement2.setString(2, type);
			ResultSet rs2 = checkStatement2.executeQuery();

			
			if (rs2.next()) {
				Integer currentAmount = rs2.getInt("amount");
				PreparedStatement updateStatement = con.prepareStatement("UPDATE inventories SET amount = ? WHERE user = ? AND type = ?");
				updateStatement.setInt(1, currentAmount + 1);
				updateStatement.setString(2, name);
				updateStatement.setString(3, type);
				updateStatement.executeUpdate();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
