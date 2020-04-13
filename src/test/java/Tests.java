import models.ReferencingSomeTable;
import models.SomeTable;
import no.netb.libjsqlite.BaseModel;
import no.netb.libjsqlite.Database;
import no.netb.libjsqlite.Jsqlite;
import no.netb.libjsqlite.JsqliteException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Tests {

    private static Database database;
    private static final List<Class<? extends BaseModel>> modelClasses;
    static {
        List<Class<? extends BaseModel>> list = new ArrayList<>();

        list.add(SomeTable.class);
        list.add(ReferencingSomeTable.class);

        modelClasses = Collections.unmodifiableList(list);
    }

    private static SomeTable someTable;
    private static ReferencingSomeTable referencingSomeTable;

    @BeforeClass
    public static void testCreateTables() throws SQLException, JsqliteException {
        database = Jsqlite.connect("test.db", false);

        database.createTablesIfNotExists(modelClasses);

        someTable = new SomeTable(420, "hello", new Timestamp(10000000000L), false, SomeTable.SomeEnum.MEMBER_A);
        database.insert(someTable);

        referencingSomeTable = new ReferencingSomeTable("hello from the other side", someTable.getId());
        database.insert(referencingSomeTable);
    }

    @Test
    public void testSelectN() throws IllegalAccessException, SQLException, InstantiationException {
        List<ReferencingSomeTable> models = database.selectN(ReferencingSomeTable.class, "WHERE r.someTableId= ?", 1);
        assertEquals("There shall be one row matching the query", 1, models.size());

        ReferencingSomeTable model = models.get(0);
        assertEquals("fields shall be identical", referencingSomeTable.getOtherText(), model.getOtherText());
        assertEquals("fields shall be identical", referencingSomeTable.getSomeTableId(), model.getSomeTableId());

        List<SomeTable> models2 = database.selectN(SomeTable.class, "WHERE s.id = ?", model.getSomeTableId());
        assertEquals("There shall be one row matching the query", 1, models2.size());

        SomeTable model2 = models2.get(0);
        assertEquals("fields shall be identical", someTable.getText(), model2.getText());
        assertEquals("fields shall be identical", someTable.getX(), model2.getX());
    }

    @Test
    public void testUpdate() throws SQLException, IllegalAccessException, InstantiationException {
        // insert some initial values
        SomeTable someTable = new SomeTable(69, "deja vu", new Timestamp(0), false, SomeTable.SomeEnum.MEMBER_A);
        database.insert(someTable);

        // update
        Timestamp newTime = new Timestamp(797979797979L);
        someTable.setX(42);
        someTable.setText("i've been in this place before");
        someTable.setTimestamp(newTime);
        someTable.setBool(true);
        someTable.setSomeEnum(SomeTable.SomeEnum.MEMBER_B);
        database.update(someTable);

        // fetch from DB and check that all fields was updated
        SomeTable someTableFetched = database.selectN(SomeTable.class, "WHERE s.id = ?", someTable.getId()).get(0);
        assertEquals(42, someTableFetched.getX());
        assertEquals("i've been in this place before", someTableFetched.getText());
        assertEquals(newTime.getTime(), someTable.getTimestamp().getTime());
        assertEquals(true, someTable.isBool());
        assertEquals(SomeTable.SomeEnum.MEMBER_B, someTable.getSomeEnum());
    }
}
