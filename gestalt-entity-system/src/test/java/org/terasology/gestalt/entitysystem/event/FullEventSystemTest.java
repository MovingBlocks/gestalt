package org.terasology.gestalt.entitysystem.event;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.component.management.ComponentManager;
import org.terasology.gestalt.entitysystem.component.store.ArrayComponentStore;
import org.terasology.gestalt.entitysystem.component.store.ComponentStore;
import org.terasology.gestalt.entitysystem.component.store.ConcurrentComponentStore;
import org.terasology.gestalt.entitysystem.entity.EntityManager;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.entity.manager.CoreEntityManager;
import org.terasology.gestalt.entitysystem.event.impl.EventReceiverMethodSupport;
import org.terasology.gestalt.entitysystem.event.impl.EventSystemImpl;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.module.ModuleFactory;
import org.terasology.gestalt.module.ModuleMetadata;
import org.terasology.gestalt.module.ModuleRegistry;
import org.terasology.gestalt.module.TableModuleRegistry;
import org.terasology.gestalt.module.sandbox.JavaModuleClassLoader;
import org.terasology.gestalt.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.gestalt.module.sandbox.WarnOnlyProviderFactory;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;

import java.lang.reflect.Modifier;
import java.util.List;

import modules.test.DeleteEntityEventReceiver;
import modules.test.TestEventReceiver;
import modules.test.components.Sample;
import modules.test.events.TestEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FullEventSystemTest {

    private static final String EVENT_VALUE = "Test";
    private EntityManager entityManager;
    private EventSystem eventSystem = new EventSystemImpl();
    private EventReceiverMethodSupport eventReceiverMethodSupport = new EventReceiverMethodSupport();

    public FullEventSystemTest() {
        // Define/obtain modules
        ModuleRegistry moduleRegistry = new TableModuleRegistry();
        ModuleFactory factory = new ModuleFactory();
        factory.setScanningForClasses(true); // false for android

        ModuleMetadata testModule = new ModuleMetadata();
        testModule.setId(new Name("Test"));
        testModule.setVersion(new Version(1, 0, 0));
        moduleRegistry.add(factory.createPackageModule(testModule, "modules.test"));

        StandardPermissionProviderFactory permissionProviderFactory = new StandardPermissionProviderFactory();
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("java.lang");
        permissionProviderFactory.getBasePermissionSet().addAPIPackage("org.terasology");

        ModuleEnvironment environment = new ModuleEnvironment(moduleRegistry, new WarnOnlyProviderFactory(permissionProviderFactory), JavaModuleClassLoader::create);

        // Create component stores. This gives an opportunity to set some component types to use ArrayComponentStore
        ComponentManager componentManager = new ComponentManager();

        List<ComponentStore<?>> stores = Lists.newArrayList();
        for (Class<? extends Component> componentType : environment.getSubtypesOf(Component.class)) {
            if (!Modifier.isAbstract(componentType.getModifiers())) {
                stores.add(new ConcurrentComponentStore(new ArrayComponentStore(componentManager.getType(componentType))));
            }
        }

        // EntityManager
        entityManager = new CoreEntityManager(stores);
    }

    @Test
    public void sendEventTest() {
        Sample component = new Sample();
        EntityRef entity = entityManager.createEntity(component);

        TestEventReceiver receiver = new TestEventReceiver();
        eventReceiverMethodSupport.register(receiver, eventSystem);
        TestEvent testEvent = new TestEvent("test");
        eventSystem.send(testEvent, entity);
        assertFalse(receiver.called);

        eventSystem.processEvents();

        assertTrue(receiver.called);
        assertEquals(component, receiver.component);
        assertEquals(entity, receiver.entity);
    }

    @Test
    public void sendEventToDeletedEntityTest() {
        Sample component = new Sample();
        EntityRef entity = entityManager.createEntity(component);
        entity.delete();

        TestEventReceiver receiver = new TestEventReceiver();
        eventReceiverMethodSupport.register(receiver, eventSystem);
        TestEvent testEvent = new TestEvent("test");
        eventSystem.send(testEvent, entity);
        assertFalse(receiver.called);

        eventSystem.processEvents();

        assertFalse(receiver.called);
    }

    @Test
    public void deleteEventWhileProcessingEntityTest() {
        Sample component = new Sample();
        EntityRef entity = entityManager.createEntity(component);

        TestEventReceiver receiver = new TestEventReceiver();
        eventReceiverMethodSupport.register(receiver, eventSystem);
        DeleteEntityEventReceiver deleteReceiver = new DeleteEntityEventReceiver();
        eventReceiverMethodSupport.register(deleteReceiver, eventSystem);
        TestEvent testEvent = new TestEvent("test");
        eventSystem.send(testEvent, entity);
        eventSystem.processEvents();

        assertFalse(receiver.called);
        assertTrue(deleteReceiver.called);
    }

}
