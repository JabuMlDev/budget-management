package com.balance.service;

import java.util.List;

import com.balance.exception.ClientNotFoundException;
import com.balance.exception.InvoiceNotFoundException;
import com.balance.model.Client;
import com.balance.model.Invoice;
import com.balance.repository.ClientRepository;
import com.balance.repository.InvoiceRepository;
import com.balance.repository.TypeRepository;
import com.balance.transaction.TransactionManager;

public class InvoiceServiceTransactional implements InvoiceService{

	private TransactionManager transactionManager;
	
	public InvoiceServiceTransactional(TransactionManager transactionManager) {
		this.transactionManager=transactionManager;
	}
	
	@Override
	public List<Invoice> findAllInvoicesByYear(int year) {
		return transactionManager.doInTransaction(
				factory -> { 
					InvoiceRepository invoiceRepository=(InvoiceRepository) factory.createRepository(TypeRepository.INVOICE);
				    return invoiceRepository.findInvoicesByYear(year);
		});
	}

	@Override
	public List<Integer> findYearsOfTheInvoices() {
		return transactionManager.doInTransaction(
				factory -> { 
					InvoiceRepository invoiceRepository=(InvoiceRepository) factory.createRepository(TypeRepository.INVOICE);
				    return invoiceRepository.getYearsOfInvoicesInDatabase();
		});
	}

	@Override
	public List<Invoice> findInvoicesByClientAndYear(Client client, int year){
		return transactionManager.doInTransaction(
			factory -> {
				ClientRepository clientRepository=(ClientRepository) factory.createRepository(TypeRepository.CLIENT);
				if(clientRepository.findById(client.getId())==null) {
					throw new ClientNotFoundException("Il cliente con id "+
							client.getId()+" non è presente nel database");
				}
				InvoiceRepository invoiceRepository=(InvoiceRepository) factory.createRepository(TypeRepository.INVOICE);
			    return invoiceRepository.findInvoicesByClientAndYear(client, year);
			});
		
	}

	public Invoice addInvoice(Invoice invoice) {
		return transactionManager.doInTransaction(
				factory -> { 
					ClientRepository clientRepository=(ClientRepository) factory.createRepository(TypeRepository.CLIENT);
					if(clientRepository.findById(invoice.getClient().getId())==null) {
						throw new ClientNotFoundException("Il cliente con id "+
								invoice.getClient().getId()+" non è presente nel database");
					}
					 ((InvoiceRepository) factory.createRepository(TypeRepository.INVOICE))
						.save(invoice);
					 return invoice;
				});
	}

	public void removeInvoice(Invoice invoice) {
		transactionManager.doInTransaction(
				factory -> {
					String clientId=invoice.getClient().getId();
					if (((ClientRepository) factory.createRepository(TypeRepository.CLIENT))
							.findById(clientId)==null) {
						throw new ClientNotFoundException("Il cliente con id "+
								clientId+" non è presente nel database");
					}
					String invoiceId=invoice.getId();
					InvoiceRepository invoiceRepository=(InvoiceRepository) factory.createRepository(TypeRepository.INVOICE);
					if(invoiceRepository.findById(invoiceId)==null) {
						throw new InvoiceNotFoundException("La fattura con id "+
								invoiceId+" non è presente nel database");
					}
					return invoiceRepository.delete(invoiceId);
				});
	}
	
}
