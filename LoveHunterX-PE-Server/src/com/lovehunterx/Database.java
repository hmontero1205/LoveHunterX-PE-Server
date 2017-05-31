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
}
