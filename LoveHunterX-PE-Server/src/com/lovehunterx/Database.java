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
			//con = DriverManager.getConnection(DATABASE, USERNAME, PASSWORD);
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/hans", "root", "");
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
				PreparedStatement insStatement = con.prepareStatement("INSERT INTO users (name, pass) VALUES (?, ?)");
				insStatement.setString(1, u);
				insStatement.setString(2, p);
				insStatement.executeUpdate();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean setFurniture(String x, String y, String uid, String type, String user) {
		try {
			PreparedStatement checkStatement = con.prepareStatement("SELECT * FROM furnitures WHERE uid = ?");
			checkStatement.setInt(1, Integer.valueOf(uid));
			ResultSet rs = checkStatement.executeQuery();
			if (rs.next()){
				PreparedStatement updateStatement = con.prepareStatement("UPDATE furnitures SET x = ?, y = ? WHERE uid = ?");
				updateStatement.setFloat(1, Float.valueOf(x));
				updateStatement.setFloat(2, Float.valueOf(y));
				updateStatement.setFloat(1, Integer.valueOf(uid));
				updateStatement.executeUpdate();
				
				return true;
			} else {
				PreparedStatement checkStatement2 = con.prepareStatement("SELECT amount FROM inventories WHERE user = ? AND type = ?");
				checkStatement2.setString(1, user);
				checkStatement2.setString(2, type);
				ResultSet rs2 = checkStatement2.executeQuery();
				int currentAmount = rs2.getInt("amount");
				if(rs2.next() && currentAmount > 0) {
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
					int createdUID = lastRowStatement.executeQuery().getInt("uid");
					//return string instead of boolean?
					

				}
				
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public ResultSet getInventory(String user) {
		PreparedStatement getStatement;
		try {
			getStatement = con.prepareStatement("SELECT * FROM inventories WHERE user = ?");
			getStatement.setString(1, user);
			return getStatement.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
