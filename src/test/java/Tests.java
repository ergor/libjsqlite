import no.netb.libjcommon.result.Result;
import models.ReferencingSomeTable;
import models.SomeTable;
import no.netb.libjsqlite.BaseModel;
import no.netb.libjsqlite.Jsqlite;
import no.netb.libjsqlite.TablesInit;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tests {

    private static final List<Class<? extends BaseModel>> modelClasses;
    static {
        List<Class<? extends BaseModel>> list = new ArrayList<>();

        list.add(SomeTable.class);
        list.add(ReferencingSomeTable.class);

        modelClasses = Collections.unmodifiableList(list);
    }

    @BeforeClass
    public static void testCreateTables() {
        try {
            TablesInit.createTables(modelClasses);

            SomeTable someTable = new SomeTable(420, "hello");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSelectN() {
        Result<List<ReferencingSomeTable>, Exception> selectResult = Jsqlite.selectN(ReferencingSomeTable.class, "WHERE r.someTableId= ?", 1);
        if (selectResult.isErr()) {
            throw new RuntimeException("select failed:", selectResult.getErr().get());
        }
        List<ReferencingSomeTable> fsNodes = selectResult.unwrap();
    }
}
