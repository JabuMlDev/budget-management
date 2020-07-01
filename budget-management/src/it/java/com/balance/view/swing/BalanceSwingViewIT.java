package com.balance.view.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.balance.controller.BalanceController;
import com.balance.model.Client;
import com.balance.model.Invoice;
import com.balance.repository.mongodb.ClientMongoRepository;
import com.balance.repository.mongodb.InvoiceMongoRepository;
import com.balance.service.ClientService;
import com.balance.service.ClientServiceTransactional;
import com.balance.service.InvoiceService;
import com.balance.service.InvoiceServiceTransactional;
import com.balance.transaction.TransactionManager;
import com.balance.transaction.mongodb.TransactionMongoManager;
import com.balance.utils.DateTestsUtil;
import com.mongodb.MongoClient;
import com.mongodb.client.ClientSession;

@RunWith(GUITestRunner.class)
public class BalanceSwingViewIT extends AssertJSwingJUnitTestCase{
	
	private static final String DB_NAME="balance";
	private static final String COLLECTION_CLIENTS_NAME="client";
	private static final String COLLECTION_INVOICES_NAME="invoice";
	
	private static final String CLIENT_IDENTIFIER_1="test identifier 1";
	private static final String CLIENT_IDENTIFIER_2="test identifier 2";
	
	private static final int YEAR_FIXTURE=2019;
	private static final int CURRENT_YEAR=Calendar.getInstance().get(Calendar.YEAR);
	
	private static final Date DATE_OF_THE_YEAR_FIXTURE=DateTestsUtil.getDateFromYear(YEAR_FIXTURE);
	private static final Date DATE_OF_THE_PREVIOUS_YEAR_FIXTURE=DateTestsUtil.getDateFromYear(YEAR_FIXTURE-1);
	private static final Date DATE_OF_THE_NEXT_YEAR_FIXTURE=DateTestsUtil.getDateFromYear(YEAR_FIXTURE+1);
	
	private static final double INVOICE_REVENUE_1=10.0;
	private static final double INVOICE_REVENUE_2=20.0;
	private static final double INVOICE_REVENUE_3=40.0;
	
	private MongoClient mongoClient;
	
	private ClientMongoRepository clientRepository;
	private InvoiceMongoRepository invoiceRepository;
	private BalanceController balanceController;
	private BalanceSwingView balanceSwingView;
	
	private FrameFixture window;
	@Override
	protected void onSetUp() {
		mongoClient=new MongoClient("localhost");
		ClientSession clientSession=mongoClient.startSession();
		clientRepository=new ClientMongoRepository(mongoClient,clientSession,DB_NAME, COLLECTION_CLIENTS_NAME);
		invoiceRepository=new InvoiceMongoRepository(mongoClient,clientSession,DB_NAME, 
				COLLECTION_INVOICES_NAME, clientRepository);
		for (Client client : clientRepository.findAll()) {
			clientRepository.delete(client.getId()); 
		}
		for (Invoice invoice : invoiceRepository.findAll()) {
			invoiceRepository.delete(invoice.getId()); 
		}
		GuiActionRunner.execute(() -> {
			balanceSwingView=new BalanceSwingView();
			TransactionManager transactionManager=new TransactionMongoManager(mongoClient, 
					DB_NAME, COLLECTION_CLIENTS_NAME, COLLECTION_INVOICES_NAME);
			ClientService clientService=new ClientServiceTransactional(transactionManager);
			InvoiceService invoiceService=new InvoiceServiceTransactional(transactionManager);
			balanceController=new BalanceController(balanceSwingView, clientService, invoiceService);
			balanceSwingView.setBalanceController(balanceController);
			return balanceSwingView;
		});
		window = new FrameFixture(robot(), balanceSwingView);
		window.show();
	}
	
	@Override
	protected void onTearDown() {
		mongoClient.close();
	}
	
	
	@Test @GUITest
	public void testAllClients() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		GuiActionRunner.execute( 
				() -> balanceController.allClients() );
		assertThat(window.list("clientsList").contents())
			.containsExactly(client1.toString(),client2.toString());
	}
	
	@Test @GUITest
	public void testAllInvoicesByYear() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client2, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE, INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		GuiActionRunner.execute( () -> {
				balanceController.yearsOfTheInvoices();
				balanceController.allInvoicesByYear(YEAR_FIXTURE);
			}	
		);
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents[0]).containsExactly(invoice1.getClient().getIdentifier(),
				invoice1.getDateInString(),""+invoice1.getRevenue());
		assertThat(tableContents[1]).containsExactly(invoice2.getClient().getIdentifier(),
				invoice2.getDateInString(),""+invoice2.getRevenue());
	}
	
	@Test @GUITest
	public void testViewInvoicesAndAnnualRevenueByYear() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client2, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE, INVOICE_REVENUE_3);
		Invoice invoice4=new Invoice(client2, DATE_OF_THE_NEXT_YEAR_FIXTURE, 
				INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		invoiceRepository.save(invoice4);
		GuiActionRunner.execute( 
				() -> balanceController.yearsOfTheInvoices()
		);	
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE)); 
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents[0]).containsExactly(invoice1.getClient().getIdentifier(),
				invoice1.getDateInString(),""+invoice1.getRevenue());
		assertThat(tableContents[1]).containsExactly(invoice2.getClient().getIdentifier(),
				invoice2.getDateInString(),""+invoice2.getRevenue());
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_1+INVOICE_REVENUE_2)+"€");
	}
	
	@Test @GUITest
	public void testViewInvoicesAndAnnualRevenueByClientAndYear() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client2, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE, INVOICE_REVENUE_3);
		Invoice invoice4=new Invoice(client2, DATE_OF_THE_NEXT_YEAR_FIXTURE, 
				INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		invoiceRepository.save(invoice4);
		GuiActionRunner.execute( () -> {
				balanceController.initializeView();
			}
		);	
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE)); 
		window.list("clientsList").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents[0]).containsExactly(invoice1.getClient().getIdentifier(),
				invoice1.getDateInString(),""+invoice1.getRevenue());
		window.label("revenueLabel").requireText(
				"Il ricavo totale delle fatture del cliente " + CLIENT_IDENTIFIER_1 +
				" nel "+ YEAR_FIXTURE+ " è di "+String.format("%.2f", 
						INVOICE_REVENUE_1)+"€");
	}
	
	@Test @GUITest
	public void testViewInvoicesAndAnnualRevenueByClientAndYearWhenClientIsNotPresentInDatabase() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		invoiceRepository.deleteAllInvoicesByClient(client1.getId());
		clientRepository.delete(client1.getId());
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE)); 
		window.list("clientsList").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.textBox("paneClientErrorMessage").requireText(""
				+ "Cliente non più presente nel database: " + 
				client1.getIdentifier());
		assertThat(window.list("clientsList").contents()).doesNotContain(client1.toString());
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents[0]).containsExactly(invoice2.getClient().getIdentifier(),
				invoice2.getDateInString(),""+invoice2.getRevenue());
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_2)+"€");
	}
	
	@Test @GUITest
	public void testViewAllInvoicesAndAnnualRevenueAfterSelectingAClient() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client1, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE,
										INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		window.list("clientsList").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.button(JButtonMatcher.withText(".*Vedi tutte.*le fatture.*")).click();
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents[0]).containsExactly(invoice1.getClient().getIdentifier(),
				invoice1.getDateInString(),""+invoice1.getRevenue());	
		assertThat(tableContents[1]).containsExactly(invoice2.getClient().getIdentifier(),
				invoice2.getDateInString(),""+invoice2.getRevenue());
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_1+INVOICE_REVENUE_2)+"€");
	}
	
	@Test @GUITest
	public void testAddClientButton() {
		window.textBox("textField_clientName").enterText("test identifier");
		window.button(JButtonMatcher.withText("Aggiungi cliente")).click();
		assertThat(window.list("clientsList").contents())
			.containsOnly(new Client("test identifier").toString());
		assertThat(window.comboBox("clientsCombobox").contents())
			.containsExactly(new Client("test identifier").toString());
	}
	
	@Test @GUITest
	public void testRemoveClientButtonSuccess() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client1, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE,
										INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		window.list("clientsList").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.button(JButtonMatcher.withText("Rimuovi cliente")).click();
		assertThat(window.list("clientsList").contents())
			.noneMatch(e -> e.contains(CLIENT_IDENTIFIER_1));
		assertThat(window.comboBox("clientsCombobox").contents())
			.noneMatch(e -> e.contains(CLIENT_IDENTIFIER_1));
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).containsOnly(new String[] {invoice2.getClient().getIdentifier(),
				invoice2.getDateInString(),""+invoice2.getRevenue()});
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
					INVOICE_REVENUE_2)+"€");
	}
	
	@Test @GUITest
	public void testRemoveClientButtonError() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoiceOfClient2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		invoiceRepository.save(invoiceOfClient2);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		window.list("clientsList").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		clientRepository.delete(client1.getId());
		window.button(JButtonMatcher.withText("Rimuovi cliente")).click();
		assertThat(window.list("clientsList").contents())
			.noneMatch(e -> e.contains(CLIENT_IDENTIFIER_1));
		assertThat(window.comboBox("clientsCombobox").contents())
			.noneMatch(e -> e.contains(CLIENT_IDENTIFIER_1));
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).containsOnly(new String[] {invoiceOfClient2.getClient().getIdentifier(),
				invoiceOfClient2.getDateInString(),""+invoiceOfClient2.getRevenue()});
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
					INVOICE_REVENUE_2)+"€");
		window.textBox("paneClientErrorMessage").requireText(""
				+ "Cliente non più presente nel database: " + 
				client1.getIdentifier());
	}
	
	@Test @GUITest
	public void testAddInvoiceOfYearSelectedButtonSuccess() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client1, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE,
				INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		window.comboBox("clientsCombobox").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.textBox("textField_dayOfDateInvoice").enterText("1");
		window.textBox("textField_monthOfDateInvoice").enterText("5");
		window.textBox("textField_yearOfDateInvoice").enterText(""+YEAR_FIXTURE);
		window.textBox("textField_revenueInvoice").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi fattura")).click();
		Invoice invoiceAdded=new Invoice(client1,DateTestsUtil.getDate(1, 5, YEAR_FIXTURE),10.20);
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).contains(new String[] {invoice1.getClient().getIdentifier(),
				invoice1.getDateInString(),""+invoice1.getRevenue()});
		assertThat(tableContents).contains(new String[] {invoice2.getClient().getIdentifier(),
				invoice2.getDateInString(),""+invoice2.getRevenue()});
		assertThat(tableContents).contains(new String[] {invoiceAdded.getClient().getIdentifier(),
				invoiceAdded.getDateInString(),""+invoiceAdded.getRevenue()});
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_1+INVOICE_REVENUE_2+10.20)+"€");
	}
	
	@Test @GUITest
	public void testAddInvoiceOfNotYearSelectedButtonSuccess() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client1, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE,
				INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+(YEAR_FIXTURE-1)));
		window.comboBox("clientsCombobox").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.textBox("textField_dayOfDateInvoice").enterText("1");
		window.textBox("textField_monthOfDateInvoice").enterText("5");
		window.textBox("textField_yearOfDateInvoice").enterText(""+YEAR_FIXTURE);
		window.textBox("textField_revenueInvoice").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi fattura")).click();
		Invoice invoiceAdded=new Invoice(client1,DateTestsUtil.getDate(1, 5, YEAR_FIXTURE),10.20);
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).doesNotContain(new String[] {invoiceAdded.getClient().getIdentifier(),
						invoiceAdded.getDateInString(),""+invoiceAdded.getRevenue()}); 
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE-1)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_3)+"€");
	}
	
	@Test @GUITest
	public void testAddInvoiceOfFirstDayOfYearButtonSuccess() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));	
		invoiceRepository.save(new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1));
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		window.comboBox("clientsCombobox").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.textBox("textField_dayOfDateInvoice").enterText("1");
		window.textBox("textField_monthOfDateInvoice").enterText("1");
		window.textBox("textField_yearOfDateInvoice").enterText(""+YEAR_FIXTURE);
		window.textBox("textField_revenueInvoice").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi fattura")).click();
		Invoice invoiceAdded=new Invoice(client1,DateTestsUtil.getDate(1, 1, YEAR_FIXTURE),10.20);
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).contains(new String[] {invoiceAdded.getClient().getIdentifier(),
				invoiceAdded.getDateInString(),""+invoiceAdded.getRevenue()});
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_1+10.20)+"€");
		GuiActionRunner.execute( () -> balanceController.yearsOfTheInvoices());
		assertThat(window.comboBox("yearsCombobox").contents())
			.containsExactly(""+CURRENT_YEAR,""+YEAR_FIXTURE);
	}
	
	@Test @GUITest
	public void testAddInvoiceOfLastDayOfYearButtonSuccess() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));	
		invoiceRepository.save(new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1));
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		window.comboBox("clientsCombobox").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.textBox("textField_dayOfDateInvoice").enterText("31");
		window.textBox("textField_monthOfDateInvoice").enterText("12");
		window.textBox("textField_yearOfDateInvoice").enterText(""+YEAR_FIXTURE);
		window.textBox("textField_revenueInvoice").enterText("10.20");
		window.button(JButtonMatcher.withText("Aggiungi fattura")).click();
		
		Invoice invoiceAdded=new Invoice(client1,DateTestsUtil.getDate(31, 12, YEAR_FIXTURE),10.20);
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).contains(new String[] {invoiceAdded.getClient().getIdentifier(),
				invoiceAdded.getDateInString(),""+invoiceAdded.getRevenue()});
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_1+10.20)+"€");
		GuiActionRunner.execute( () -> balanceController.yearsOfTheInvoices());
		assertThat(window.comboBox("yearsCombobox").contents())
			.containsExactly(""+CURRENT_YEAR,""+YEAR_FIXTURE);
	}
	
	@Test @GUITest
	public void testAddInvoiceButtonErrorNoExistingClient() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		Invoice invoice3=new Invoice(client1, DATE_OF_THE_PREVIOUS_YEAR_FIXTURE,
				INVOICE_REVENUE_3);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		invoiceRepository.save(invoice3);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+(YEAR_FIXTURE)));
		window.comboBox("clientsCombobox").selectItem(Pattern.compile(CLIENT_IDENTIFIER_1));
		window.textBox("textField_dayOfDateInvoice").enterText("1");
		window.textBox("textField_monthOfDateInvoice").enterText("5");
		window.textBox("textField_yearOfDateInvoice").enterText(""+YEAR_FIXTURE);
		window.textBox("textField_revenueInvoice").enterText("10.20");
		clientRepository.delete(client1.getId());
		window.button(JButtonMatcher.withText("Aggiungi fattura")).click();
		window.textBox("paneClientErrorMessage").requireText(""
				+ "Cliente non più presente nel database: " + 
				client1.getIdentifier());
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).containsOnly(new String[] {invoice2.getClient().getIdentifier(),
				invoice2.getDateInString(),""+invoice2.getRevenue()});
		assertThat(window.list("clientsList").contents())
			.noneMatch( e -> e.contains(client1.toString()));
		assertThat(window.comboBox("clientsCombobox").contents())
			.noneMatch( e -> e.contains(client1.toString()));
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						INVOICE_REVENUE_2)+"€");
	}
	
	@Test @GUITest
	public void testRemoveInvoiceButtonSuccess() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoice1=new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1);
		Invoice invoice2=new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2);
		invoiceRepository.save(invoice1);
		invoiceRepository.save(invoice2);
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		int rowInvoiceToDeleted=balanceSwingView.getInvoiceTableModel().getRowInvoice(invoice1);
		window.table("invoicesTable").selectRows(rowInvoiceToDeleted);
		window.button(JButtonMatcher.withText(".*Rimuovi.*fattura.*")).click();
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).doesNotContain(new String[] {invoice1.getClient().getIdentifier(),
				invoice1.getDateInString(),""+invoice1.getRevenue()}); 
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
					INVOICE_REVENUE_2)+"€");
	}
	
	@Test @GUITest
	public void testRemoveInvoiceButtonErrorForNoExistingInvoiceInDatabase() {
		Client client1=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client client2=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoiceRemaining=invoiceRepository.save(
				new Invoice(client2, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2));
		Invoice invoiceToDeleted=invoiceRepository.save(
				new Invoice(client1, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1));
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		int rowInvoiceToDeleted=balanceSwingView.getInvoiceTableModel().getRowInvoice(invoiceToDeleted);
		window.table("invoicesTable").selectRows(rowInvoiceToDeleted);
		invoiceRepository.delete(invoiceToDeleted.getId());
		window.button(JButtonMatcher.withText(".*Rimuovi.*fattura.*")).click();
		window.textBox("paneInvoiceErrorMessage").requireText("Fattura non più presente nel database: "
				+invoiceToDeleted.toString());
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).doesNotContain(new String[] {invoiceToDeleted.getClient().getIdentifier(),
				invoiceToDeleted.getDateInString(),""+invoiceToDeleted.getRevenue()}); 
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						invoiceRemaining.getRevenue())+"€");
	}
	
	@Test @GUITest
	public void testRemoveInvoiceButtonErrorForNoExistingClientInDatabase() {
		Client clientRemaining=clientRepository.save(new Client(CLIENT_IDENTIFIER_1));
		Client clientToDeleted=clientRepository.save(new Client(CLIENT_IDENTIFIER_2));
		Invoice invoiceClientRemaining=invoiceRepository.save(
				new Invoice(clientRemaining, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_2));
		Invoice invoiceClientToDeleted=invoiceRepository.save(
				new Invoice(clientToDeleted, DATE_OF_THE_YEAR_FIXTURE, INVOICE_REVENUE_1));
		GuiActionRunner.execute( () -> balanceController.initializeView() );
		window.comboBox("yearsCombobox")
			.selectItem(Pattern.compile(""+YEAR_FIXTURE));
		int rowInvoiceToDeleted=balanceSwingView.getInvoiceTableModel().getRowInvoice(invoiceClientToDeleted);
		window.table("invoicesTable").selectRows(rowInvoiceToDeleted);
		clientRepository.delete(clientToDeleted.getId());
		window.button(JButtonMatcher.withText(".*Rimuovi.*fattura.*")).click();
		window.textBox("paneClientErrorMessage").requireText("Cliente non più presente nel database: "
				+clientToDeleted.toString());
		assertThat(window.list("clientsList").contents())
			.noneMatch( e -> e.contains(clientToDeleted.toString()));
		assertThat(window.comboBox("clientsCombobox").contents())
			.noneMatch( e -> e.contains(clientToDeleted.toString()));
		String[][] tableContents = window.table("invoicesTable").contents(); 
		assertThat(tableContents).doesNotContain(new String[] {invoiceClientToDeleted.getClient().getIdentifier(),
				invoiceClientToDeleted.getDateInString(),""+invoiceClientToDeleted.getRevenue()}); 
		window.label("revenueLabel").requireText(
				"Il ricavo totale del "+(YEAR_FIXTURE)+" è di "+String.format("%.2f", 
						invoiceClientRemaining.getRevenue())+"€");
	}
	
}
