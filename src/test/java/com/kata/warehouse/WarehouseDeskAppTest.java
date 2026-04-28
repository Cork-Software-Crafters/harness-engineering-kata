package com.kata.warehouse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseDeskAppTest {
    private WarehouseDeskApp app;

    @BeforeEach
    void setUp() {
        app = new WarehouseDeskApp();
        app.seedData();
    }

    @Test
    void seedData_shouldInitializeStockLevels() {
        app = new WarehouseDeskApp();
        app.seedData();
        
        app.processLine("COUNT;PEN-BLACK");
        app.processLine("COUNT;PEN-BLUE");
        app.processLine("COUNT;NOTE-A5");
        app.processLine("COUNT;STAPLER");
        
        assertThat(app.getEventLog()).contains(
            "count PEN-BLACK onHand=40 reserved=0 available=40",
            "count PEN-BLUE onHand=25 reserved=0 available=25",
            "count NOTE-A5 onHand=15 reserved=0 available=15",
            "count STAPLER onHand=4 reserved=0 available=4"
        );
    }

    @Test
    void seedData_shouldInitializePrices() {
        app = new WarehouseDeskApp();
        app.seedData();
        
        app.processLine("SELL;customer;PEN-BLACK;1");
        app.processLine("SELL;customer;PEN-BLUE;1");
        app.processLine("SELL;customer;NOTE-A5;1");
        app.processLine("SELL;customer;STAPLER;1");
        
        assertThat(app.getEventLog()).contains(
            "order O1001 shipped to customer amount=1.5",
            "order O1002 shipped to customer amount=1.6",
            "order O1003 shipped to customer amount=4.0",
            "order O1004 shipped to customer amount=12.0"
        );
    }

    @Test
    void seedData_shouldInitializeCashBalance() {
        app = new WarehouseDeskApp();
        app.seedData();
        
        assertThat(app.getCashBalance()).isEqualTo(300.0);
    }

    @Test
    void seedData_shouldInitializeOrderNumber() {
        app = new WarehouseDeskApp();
        app.seedData();
        
        app.processLine("SELL;customer;PEN-BLACK;1");
        
        assertThat(app.getEventLog()).anySatisfy(event -> 
            assertThat(event).contains("O1001")
        );
    }

    @Test
    void RECV_command_shouldIncreaseStockAndDecreaseCash() {
        app.processLine("RECV;NOTE-A5;5;2.20");
        app.processLine("COUNT;NOTE-A5");
        
        assertThat(app.getEventLog()).contains(
            "received 5 of NOTE-A5 at 2.2",
            "count NOTE-A5 onHand=20 reserved=0 available=20"
        );
        assertThat(app.getCashBalance()).isEqualTo(300.0 - (5 * 2.20));
    }

    @Test
    void RECV_command_shouldAddNewSku() {
        app.processLine("RECV;NEW-ITEM;10;5.0");
        app.processLine("COUNT;NEW-ITEM");
        
        assertThat(app.getEventLog()).contains(
            "received 10 of NEW-ITEM at 5.0",
            "count NEW-ITEM onHand=10 reserved=0 available=10"
        );
    }

    @Test
    void SELL_command_withSufficientStock_shouldShipOrder() {
        app.processLine("SELL;alice;PEN-BLACK;10");
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "order O1001 shipped to alice amount=15.0",
            "count PEN-BLACK onHand=30 reserved=0 available=30"
        );
        assertThat(app.getCashBalance()).isEqualTo(300.0 + 15.0);
    }

    @Test
    void SELL_command_withInsufficientStock_shouldBackorder() {
        app.processLine("SELL;bob;STAPLER;5");
        
        assertThat(app.getEventLog()).contains(
            "order O1001 backordered for bob sku=STAPLER qty=5"
        );
        assertThat(app.getCashBalance()).isEqualTo(300.0);
    }

    @Test
    void SELL_command_shouldIncrementOrderNumber() {
        app.processLine("SELL;alice;PEN-BLACK;1");
        app.processLine("SELL;bob;PEN-BLUE;1");
        
        assertThat(app.getEventLog()).anySatisfy(event -> 
            assertThat(event).contains("O1001")
        );
        assertThat(app.getEventLog()).anySatisfy(event -> 
            assertThat(event).contains("O1002")
        );
    }

    @Test
    void CANCEL_command_onBackorder_shouldCancelOrder() {
        app.processLine("SELL;bob;STAPLER;5");
        app.processLine("CANCEL;O1001");
        
        assertThat(app.getEventLog()).contains(
            "order O1001 backordered for bob sku=STAPLER qty=5",
            "cancelled backorder O1001"
        );
    }

    @Test
    void CANCEL_command_onShippedOrder_shouldRestockAndRefund() {
        app.processLine("SELL;alice;PEN-BLACK;10");
        app.processLine("CANCEL;O1001");
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "order O1001 shipped to alice amount=15.0",
            "cancelled shipped order O1001 with restock",
            "count PEN-BLACK onHand=40 reserved=0 available=40"
        );
        assertThat(app.getCashBalance()).isEqualTo(300.0);
    }

    @Test
    void CANCEL_command_onNonExistentOrder_shouldLogError() {
        app.processLine("CANCEL;O9999");
        
        assertThat(app.getEventLog()).contains(
            "cannot cancel O9999 because it does not exist"
        );
    }

    @Test
    void CANCEL_command_onAlreadyCancelledOrder_shouldLogError() {
        app.processLine("SELL;alice;PEN-BLACK;10");
        app.processLine("CANCEL;O1001");
        app.processLine("CANCEL;O1001");
        
        assertThat(app.getEventLog()).contains(
            "order O1001 could not be cancelled from state CANCELLED_AFTER_SHIP"
        );
    }

    @Test
    void COUNT_command_shouldShowOnHandReservedAndAvailable() {
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "count PEN-BLACK onHand=40 reserved=0 available=40"
        );
    }

    @Test
    void COUNT_command_onNonExistentSku_shouldShowZero() {
        app.processLine("COUNT;NON-EXISTENT");
        
        assertThat(app.getEventLog()).contains(
            "count NON-EXISTENT onHand=0 reserved=0 available=0"
        );
    }

    @Test
    void DUMP_command_shouldPrintState() {
        app.processLine("DUMP");
        
        assertThat(app.getEventLog()).hasSize(0);
    }

    @Test
    void unknownCommand_shouldLogError() {
        app.processLine("UNKNOWN;COMMAND");
        
        assertThat(app.getEventLog()).contains(
            "unknown command: UNKNOWN;COMMAND"
        );
    }


    @Test
    void runDemoDay_shouldExecuteAllCommands() {
        app.runDemoDay();
        
        assertThat(app.getEventLog()).isNotEmpty();
        assertThat(app.getEventLog()).anySatisfy(event -> 
            assertThat(event).contains("received")
        );
        assertThat(app.getEventLog()).anySatisfy(event -> 
            assertThat(event).contains("shipped")
        );
    }

    @Test
    void RESERVE_command_withSufficientStock_shouldCreateReservation() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;30");
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 30 minutes",
            "count PEN-BLACK onHand=40 reserved=10 available=30"
        );
    }

    @Test
    void RESERVE_command_withInsufficientStock_shouldFail() {
        app.processLine("RESERVE;alice;STAPLER;10;30");
        
        assertThat(app.getEventLog()).contains(
            "cannot reserve 10 of STAPLER for alice - only 4 available"
        );
    }

    @Test
    void RESERVE_command_shouldIncrementReservationNumber() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;30");
        app.processLine("RESERVE;bob;PEN-BLUE;5;30");
        
        assertThat(app.getEventLog()).anySatisfy(event -> 
            assertThat(event).contains("R5001")
        );
        assertThat(app.getEventLog()).anySatisfy(event -> 
            assertThat(event).contains("R5002")
        );
    }

    @Test
    void RESERVE_command_shouldAccountForExistingReservations() {
        app.processLine("RESERVE;alice;PEN-BLACK;20;30");
        app.processLine("RESERVE;bob;PEN-BLACK;15;30");
        app.processLine("RESERVE;carol;PEN-BLACK;10;30");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=20 expires in 30 minutes",
            "reservation R5002 created for bob sku=PEN-BLACK qty=15 expires in 30 minutes",
            "cannot reserve 10 of PEN-BLACK for carol - only 5 available"
        );
    }

    @Test
    void CONFIRM_command_shouldConvertReservationToShippedOrder() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;30");
        app.processLine("CONFIRM;R5001");
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 30 minutes",
            "reservation R5001 confirmed and shipped as O1001 to alice amount=15.0",
            "count PEN-BLACK onHand=30 reserved=0 available=30"
        );
        assertThat(app.getCashBalance()).isEqualTo(315.0);
    }

    @Test
    void CONFIRM_command_onNonExistentReservation_shouldFail() {
        app.processLine("CONFIRM;R9999");
        
        assertThat(app.getEventLog()).contains(
            "cannot confirm R9999 because it does not exist"
        );
    }

    @Test
    void CONFIRM_command_shouldRemoveReservation() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;30");
        app.processLine("CONFIRM;R5001");
        app.processLine("CONFIRM;R5001");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 30 minutes",
            "reservation R5001 confirmed and shipped as O1001 to alice amount=15.0",
            "cannot confirm R5001 because it does not exist"
        );
    }

    @Test
    void RELEASE_command_shouldReleaseStockBackToAvailability() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;30");
        app.processLine("RELEASE;R5001");
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 30 minutes",
            "reservation R5001 released for alice sku=PEN-BLACK qty=10",
            "count PEN-BLACK onHand=40 reserved=0 available=40"
        );
    }

    @Test
    void RELEASE_command_onNonExistentReservation_shouldFail() {
        app.processLine("RELEASE;R9999");
        
        assertThat(app.getEventLog()).contains(
            "cannot release R9999 because it does not exist"
        );
    }

    @Test
    void RELEASE_command_shouldRemoveReservation() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;30");
        app.processLine("RELEASE;R5001");
        app.processLine("RELEASE;R5001");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 30 minutes",
            "reservation R5001 released for alice sku=PEN-BLACK qty=10",
            "cannot release R5001 because it does not exist"
        );
    }

    @Test
    void expiredReservations_shouldReleaseStockAutomatically() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;0");
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 0 minutes",
            "reservation R5001 expired for alice sku=PEN-BLACK qty=10",
            "count PEN-BLACK onHand=40 reserved=0 available=40"
        );
    }

    @Test
    void expiredReservations_shouldPreventConfirmation() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;0");
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        app.processLine("CONFIRM;R5001");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 0 minutes",
            "reservation R5001 expired for alice sku=PEN-BLACK qty=10",
            "cannot confirm R5001 because it does not exist"
        );
    }

    @Test
    void multipleReservationsForSameSku_shouldTrackCorrectly() {
        app.processLine("RESERVE;alice;PEN-BLACK;10;30");
        app.processLine("RESERVE;bob;PEN-BLACK;10;30");
        app.processLine("RESERVE;carol;PEN-BLACK;10;30");
        app.processLine("RESERVE;dan;PEN-BLACK;5;30");
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=10 expires in 30 minutes",
            "reservation R5002 created for bob sku=PEN-BLACK qty=10 expires in 30 minutes",
            "reservation R5003 created for carol sku=PEN-BLACK qty=10 expires in 30 minutes",
            "reservation R5004 created for dan sku=PEN-BLACK qty=5 expires in 30 minutes",
            "count PEN-BLACK onHand=40 reserved=35 available=5"
        );
    }

    @Test
    void releasingReservation_shouldAllowNewReservation() {
        app.processLine("RESERVE;alice;PEN-BLACK;30;30");
        app.processLine("RELEASE;R5001");
        app.processLine("RESERVE;bob;PEN-BLACK;30;30");
        app.processLine("COUNT;PEN-BLACK");
        
        assertThat(app.getEventLog()).contains(
            "reservation R5001 created for alice sku=PEN-BLACK qty=30 expires in 30 minutes",
            "reservation R5001 released for alice sku=PEN-BLACK qty=30",
            "reservation R5002 created for bob sku=PEN-BLACK qty=30 expires in 30 minutes",
            "count PEN-BLACK onHand=40 reserved=30 available=10"
        );
    }
}
