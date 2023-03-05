package cn.cerc.mis.client;

import java.util.Objects;
import java.util.function.Consumer;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.db.core.ISession;

public class ServiceProxy implements IHandle {
    private DataSet dataIn;
    private DataSet dataOut;
    private ISession session;

    public final boolean isOk() {
        Objects.requireNonNull(dataOut);
        return dataOut.state() > 0;
    }

    public final boolean isOkElseThrow() throws ServiceExecuteException {
        if (!isOk())
            throw new ServiceExecuteException(dataOut.message());
        return true;
    }

    public final boolean isFail() {
        Objects.requireNonNull(dataOut);
        return dataOut.state() <= 0;
    }

    public final boolean isFail(Consumer<String> action) {
        Objects.requireNonNull(dataOut);
        boolean result = dataOut.state() <= 0;
        if (action != null)
            action.accept(dataOut.message());
        return result;
    }

    public final DataSet getDataOutElseThrow() throws ServiceExecuteException {
        Objects.requireNonNull(dataOut);
        if (dataOut.state() <= 0)
            throw new ServiceExecuteException(dataOut.message());
        return dataOut;
    }

    public final DataRow getHeadOutElseThrow() throws ServiceExecuteException {
        Objects.requireNonNull(dataOut);
        if (dataOut.state() <= 0)
            throw new ServiceExecuteException(dataOut.message());
        return dataOut.head();
    }

    public String message() {
        return dataOut().message();
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

    public final DataSet dataOut() {
        if (this.dataOut == null)
            this.dataOut = new DataSet();
        return dataOut;
    }

    protected void setDataOut(DataSet dataOut) {
        this.dataOut = dataOut;
    }

    public final DataSet dataIn() {
        if (this.dataIn == null)
            this.dataIn = new DataSet();
        return dataIn;
    }

    protected void setDataIn(DataSet dataIn) {
        this.dataIn = dataIn;
    }

}
