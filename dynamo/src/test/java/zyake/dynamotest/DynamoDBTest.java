package zyake.dynamotest;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDeleteExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;

import org.junit.Test;

public class DynamoDBTest {
    
    @Test
    public void testGetInvoiceByLoad() {    
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
            .build(); 
        final DynamoDBMapper mapper = new DynamoDBMapper(client);

        final Invoice invoice = mapper.load(Invoice.class, "I92551", "I92551");
        System.out.println(invoice);
    }

    @Test
    public void testGetBillByLoad() {    
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
            .build(); 
        final DynamoDBMapper mapper = new DynamoDBMapper(client);

        final Bill bill = mapper.load(Bill.class, "B4224663", "B4224663");
        System.out.println(bill);
    }

    @Test
    public void testGetBillWithInvoicesByQuery() {    
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
            .build(); 
        final DynamoDBMapper mapper = new DynamoDBMapper(client);

        final HashMap<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":v1",  new AttributeValue().withS("I92551"));
        eav.put(":v2",  new AttributeValue().withS("B"));

        final DynamoDBQueryExpression<BillWithInvoice> queryExpression = new DynamoDBQueryExpression<BillWithInvoice>()
            .withKeyConditionExpression("pk = :v1 and begins_with(gsi, :v2)")
            .withExpressionAttributeValues(eav);
        
        final PaginatedQueryList<BillWithInvoice> billWithInvoices = mapper.query(BillWithInvoice.class, queryExpression);
        for ( int i = 0 ; i < billWithInvoices.size() ; i ++ ) {
            System.out.println(billWithInvoices.get(i));
        }
    }

    @Test
    public void testUpdateInvoiceBySave() {    
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
            .build(); 
        final DynamoDBMapper mapper = new DynamoDBMapper(client);

        final Invoice invoice = mapper.load(Invoice.class, "I92551", "I92551");
        invoice.setIAttr1(invoice.getIAttr1() + 1);

        final DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
        final Map<String,ExpectedAttributeValue> expected = new HashMap<>();
        expected.put("pk", new ExpectedAttributeValue().withExists(true).withValue(new AttributeValue().withS("I92551")));
        expected.put("gsi", new ExpectedAttributeValue().withExists(true).withValue(new AttributeValue().withS("I92551")));
        saveExpression.setExpected(expected);

        mapper.save(invoice, saveExpression);

        System.out.println(invoice);
    }

    @Test
    public void testCreateInvoiceBySave() {    
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
            .build(); 
        final DynamoDBMapper mapper = new DynamoDBMapper(client);
        {
            final Invoice invoice = mapper.load(Invoice.class, "I92552", "I92552");
            if (invoice != null) {
                mapper.delete(invoice);
            }
        }

        final Invoice invoice = new Invoice();
        invoice.setIAttr1(100);
        invoice.setPartitionKey("I92552");
        invoice.setInvoiceId("I92552");

        final DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
        final Map<String,ExpectedAttributeValue> expected = new HashMap<>();
        expected.put("pk", new ExpectedAttributeValue().withExists(false));
        expected.put("gsi", new ExpectedAttributeValue().withExists(false));
        saveExpression.setExpected(expected);

        mapper.save(invoice, saveExpression);

        final DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression();
        final Map<String,ExpectedAttributeValue> deleteExpected = new HashMap<>();
        deleteExpected.put("pk", new ExpectedAttributeValue().withExists(true).withValue(new AttributeValue().withS(invoice.getPartitionKey())));
        deleteExpected.put("gsi", new ExpectedAttributeValue().withExists(true).withValue(new AttributeValue().withS(invoice.getInvoiceId())));
        deleteExpression.setExpected(deleteExpected);

        mapper.delete(invoice, deleteExpression);

        System.out.println(invoice);
    }

    @Test
    public void testCreateBillWithInvoicesBytransactionWrite() {    
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
            .build(); 
        final DynamoDBMapper mapper = new DynamoDBMapper(client);
        {
            final Invoice invoice = mapper.load(Invoice.class, "I92552", "I92552");
            if (invoice != null) {
                mapper.delete(invoice);
            }
        }

        final Invoice invoice = new Invoice();
        invoice.setIAttr1(100);
        invoice.setPartitionKey("I92552");
        invoice.setInvoiceId("I92552");

        final DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
        final Map<String,ExpectedAttributeValue> expected = new HashMap<>();
        expected.put("pk", new ExpectedAttributeValue().withExists(false));
        expected.put("gsi", new ExpectedAttributeValue().withExists(false));
        saveExpression.setExpected(expected);

        mapper.save(invoice, saveExpression);

        final DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression();
        final Map<String,ExpectedAttributeValue> deleteExpected = new HashMap<>();
        deleteExpected.put("pk", new ExpectedAttributeValue().withExists(true).withValue(new AttributeValue().withS(invoice.getPartitionKey())));
        deleteExpected.put("gsi", new ExpectedAttributeValue().withExists(true).withValue(new AttributeValue().withS(invoice.getInvoiceId())));
        deleteExpression.setExpected(deleteExpected);

        mapper.delete(invoice, deleteExpression);

        System.out.println(invoice);
    }

    @DynamoDBTable(tableName="InvoiceAndBill")
    public static class Invoice {

        private String partitionKey;

        private String invoiceId;

        private int IAttr1;

        private Long version;

        @DynamoDBRangeKey(attributeName="gsi")        
        public String getInvoiceId() {
            return invoiceId;
        }

        @DynamoDBVersionAttribute
        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        @DynamoDBHashKey(attributeName="pk")
        public String getPartitionKey() {
            return partitionKey;
        }

        public void setPartitionKey(String partitionKey) {
            this.partitionKey = partitionKey;
        }

        @DynamoDBAttribute(attributeName="IAttr1")
        public int getIAttr1() {
            return IAttr1;
        }

        public void setIAttr1(int iAttr1) {
            this.IAttr1 = iAttr1;
        }

        public void setInvoiceId(String invoiceId) {
            this.invoiceId = invoiceId;
        }

        @Override
        public String toString() {
            return String.format("pkey=%s, %s, IAttr1=%s, version=%d", partitionKey, invoiceId, IAttr1, version);
        }
    }

    @DynamoDBTable(tableName="InvoiceAndBill")
    public static class BillWithInvoice {

        private String invoiceId;

        private String billId;

        private int bAttr1;

        private int version;

        @DynamoDBHashKey(attributeName = "pk")
        public String getInvoiceId() {
            return invoiceId;
        }

        @DynamoDBVersionAttribute
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        @DynamoDBAttribute(attributeName="BAttr1")
        public int getBAttr1() {
            return bAttr1;
        }

        public void setBAttr1(int bAttr1) {
            this.bAttr1 = bAttr1;
        }

        @DynamoDBRangeKey(attributeName="gsi")        
        public String getBillId() {
            return billId;
        }

        public void setBillId(String billId) {
            this.billId = billId;
        }

        public void setInvoiceId(String invoiceId) {
            this.invoiceId = invoiceId;
        }

        @Override
        public String toString() {
            return String.format("pkey=%s, %s, BAttr1=%s, version=%d", invoiceId, billId, bAttr1, version);
        }
    }

    @DynamoDBTable(tableName="InvoiceAndBill")
    public static class Bill {

        private String partitionKey;

        private String billId;

        private int bAttr2;

        @DynamoDBHashKey(attributeName = "pk")
        public String getPartitionKey() {
            return partitionKey;
        }

        @DynamoDBAttribute(attributeName="BAttr2")
        public int getbAttr2() {
            return bAttr2;
        }

        public void setbAttr2(int bAttr2) {
            this.bAttr2 = bAttr2;
        }

        @DynamoDBRangeKey(attributeName="gsi") 
        public String getBillId() {
            return billId;
        }

        public void setBillId(String billId) {
            this.billId = billId;
        }

        public void setPartitionKey(String partitionKey) {
            this.partitionKey = partitionKey;
        }

        @Override
        public String toString() {
            return String.format("pkey=%s, %s, BAttr2=%s", partitionKey, billId, bAttr2);
        }
    }

}
