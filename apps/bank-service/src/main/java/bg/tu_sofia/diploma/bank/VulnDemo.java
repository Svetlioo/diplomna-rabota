package bg.tu_sofia.diploma.bank;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class VulnDemo {

    public ResultSet findByOwner(Connection connection, String ownerId) throws Exception {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM accounts WHERE owner_id = ?");
        statement.setString(1, ownerId);
        return statement.executeQuery();
    }
}
