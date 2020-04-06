import models.ReferencingSomeTable;
import models.SomeTable;
import no.netb.libjcommon.result.Result;
import no.netb.libjsqlite.BaseModel;
import no.netb.libjsqlite.Jsqlite;
import no.netb.libjsqlite.TablesInit;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Tests {

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
    public static void testCreateTables() {
        throwIfErr(TablesInit.createTablesIfNotExists(modelClasses));

        someTable = new SomeTable(420, "hello", new Timestamp(10000000000L), false);
        throwIfErr(Jsqlite.insert(someTable));
        //insertResult.getOk().flatMap(DbOkAction::commit).ifPresent(Exception::printStackTrace);

        referencingSomeTable = new ReferencingSomeTable("hello from the other side", someTable.getId());
        throwIfErr(Jsqlite.insert(referencingSomeTable));
    }

    @Test
    public void testSelectN() {
        Result<List<ReferencingSomeTable>, Exception> selectResult = Jsqlite.selectN(ReferencingSomeTable.class, "WHERE r.someTableId= ?", 1);
        throwIfErr(selectResult);

        List<ReferencingSomeTable> models = selectResult.unwrap();
        assertEquals("There shall be one row matching the query", 1, models.size());

        ReferencingSomeTable model = models.get(0);
        assertEquals("fields shall be identical", referencingSomeTable.getOtherText(), model.getOtherText());
        assertEquals("fields shall be identical", referencingSomeTable.getSomeTableId(), model.getSomeTableId());

        Result<List<SomeTable>, Exception> selectResult2 = Jsqlite.selectN(SomeTable.class, "WHERE s.id = ?", model.getSomeTableId());
        throwIfErr(selectResult2);

        List<SomeTable> models2 = selectResult2.unwrap();
        assertEquals("There shall be one row matching the query", 1, models2.size());

        SomeTable model2 = models2.get(0);
        assertEquals("fields shall be identical", someTable.getText(), model2.getText());
        assertEquals("fields shall be identical", someTable.getX(), model2.getX());
    }

    @Test
    public void testUpdate() {
        SomeTable someTable = new SomeTable(69, "deja vu", new Timestamp(0), false);
        throwIfErr(Jsqlite.insert(someTable));

        Timestamp newTime = new Timestamp(797979797979L);
        someTable.setX(42);
        someTable.setText("i've been in this place before");
        someTable.setTimestamp(newTime);
        someTable.setBool(true);
        throwIfErr(Jsqlite.update(someTable));

        Result<List<SomeTable>, Exception> selectResult = Jsqlite.selectN(SomeTable.class, "WHERE s.id = ?", someTable.getId());
        throwIfErr(selectResult);
        SomeTable someTableFetched = selectResult.unwrap().get(0);
        assertEquals(42, someTableFetched.getX());
        assertEquals("i've been in this place before", someTableFetched.getText());
        assertEquals(newTime.getTime(), someTable.getTimestamp().getTime());
        assertEquals(true, someTable.isBool());
    }

    private static void throwIfErr(Result<?, ? extends Exception> result) {
        result.getErr().ifPresent(e -> {throw new RuntimeException(e);});
    }
}
