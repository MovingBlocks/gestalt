package org.terasology.gestalt.di;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class BeanTransaction implements AutoCloseable {

    private static class ContextTransaction {
        public final Map<BeanIdentifier, Object> boundObjects = new ConcurrentHashMap<>();
    }
    private Map<BeanContext, ContextTransaction> transactionMap = new HashMap<>();
    private boolean isCommitted = false;

    <T> Optional<T> bind(BeanContext context, BeanIdentifier identifier, Supplier<T> supplier) throws Exception {
        ContextTransaction transaction = transactionMap.computeIfAbsent(context, (k) -> new ContextTransaction());
        if (context instanceof DefaultBeanContext) {
            if (((DefaultBeanContext) context).boundObjects.containsKey(identifier)) {
                return Optional.of((T) ((DefaultBeanContext) context).boundObjects.get(identifier));
            }
            if (transaction.boundObjects.containsKey(identifier)) {
                return Optional.of((T) transaction.boundObjects.get(identifier));
            }
            T result = supplier.get();
            transaction.boundObjects.put(identifier, result);
            return Optional.of(result);
        }
        throw new Exception("Unknown context type: " + context.getClass());
    }

    protected void commit() throws Exception {
        isCommitted = true;
        for (Map.Entry<BeanContext, ContextTransaction> transactionEntry : transactionMap.entrySet()) {
            BeanContext context = transactionEntry.getKey();
            ContextTransaction trans = transactionEntry.getValue();
            if (context instanceof DefaultBeanContext) {
                ((DefaultBeanContext) context).boundObjects.putAll(trans.boundObjects);
            } else {
                throw new Exception("Unknown context type: " + context.getClass());
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (!isCommitted) {
            for (ContextTransaction transaction : transactionMap.values()) {
                for (Object o : transaction.boundObjects.values()) {
                    if (o instanceof AutoCloseable) {
                        ((AutoCloseable) o).close();
                    }
                }

            }
        }
    }


}
