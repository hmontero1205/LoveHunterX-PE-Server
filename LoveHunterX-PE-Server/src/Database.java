import java.sql.*;

public class Database {
	private Connection con;
	
	public Database() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/hans", "root", "");
//			Statement stmt = con.createStatement();
//			ResultSet rs = stmt.executeQuery("select * from users");
//			while (rs.next())
//				System.out.println(rs.getString(1) + "  " + rs.getInt(2) + "  " + rs.getInt(3));
//			con.close();
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
			PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE name = ? AND pass = ?");
			ps.setString(1, u);
			ps.setString(2, p);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				return false;
			else {
				PreparedStatement insStatement = con.prepareStatement("INSERT INTO users (name, pass) VALUES (?, ?)");
				insStatement.setString(1, u);
				insStatement.setString(2, p);
				insStatement.executeUpdate();
				return true;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
