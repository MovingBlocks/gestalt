package org.terasology.gestalt.di;

import org.terasology.context.exception.CloseBeanException;
import org.terasology.gestalt.di.exceptions.UnknownContextTypeException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BeanTransaction implements AutoCloseable {

    private static class ContextTransaction {
        public final Map<BeanIdentifier, Object> boundObjects = new ConcurrentHashMap<>();
    }
    private final Map<BeanContext, ContextTransaction> transactionMap = new HashMap<>();
    private boolean isCommitted = false;

    <T> Optional<T> bind(BeanContext context, BeanIdentifier identifier, Supplier<Optional<T>> supplier) throws UnknownContextTypeException {
        ContextTransaction transaction = transactionMap.computeIfAbsent(context, k -> new ContextTransaction());
        if (context instanceof DefaultBeanContext) {
            if (((DefaultBeanContext) context).boundObjects.containsKey(identifier)) {
                return Optional.of((T) ((DefaultBeanContext) context).boundObjects.get(identifier));
            }
            if (transaction.boundObjects.containsKey(identifier)) {
                return Optional.of((T) transaction.boundObjects.get(identifier));
            }
            Optional<T> result = supplier.get();
            result.ifPresent(t -> transaction.boundObjects.put(identifier, t));
            return result;
        }
        throw new UnknownContextTypeException(context);
    }

    protected void commit() throws UnknownContextTypeException {
        isCommitted = true;
        for (Map.Entry<BeanContext, ContextTransaction> transactionEntry : transactionMap.entrySet()) {
            BeanContext context = transactionEntry.getKey();
            ContextTransaction trans = transactionEntry.getValue();
            if (context instanceof DefaultBeanContext) {
                ((DefaultBeanContext) context).boundObjects.putAll(trans.boundObjects);
            } else {
                throw new UnknownContextTypeException(context);
            }
        }
    }

    @Override
    public void close() {
        if (!isCommitted) {
            for (ContextTransaction transaction : transactionMap.values()) {
                for (Object o : transaction.boundObjects.values()) {
                    if (o instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) o).close();
                        } catch (Exception e){
                            throw new CloseBeanException("Cannot close bound bean", e);
                        }
                    }
                }
            }
        }
    }
}
