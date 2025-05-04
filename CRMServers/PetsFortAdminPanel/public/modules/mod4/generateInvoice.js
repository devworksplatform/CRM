// (Paste the entire JavaScript code from the previous response here)
function generateSummaryRows(rowsData, colCountEmpty) {
    const tableBody = document.createElement('tbody');
  
    rowsData.forEach(rowData => {
      const row = document.createElement('tr');
      if (rowData.isBlank) {
        const cell = document.createElement('td');
        cell.setAttribute('colspan', rowData.colspan);
        row.appendChild(cell);
      } else {
        const emptyCell = document.createElement('td');
        row.appendChild(emptyCell);
  
        const labelCell = document.createElement('td');
        labelCell.textContent = rowData.label;
        row.appendChild(labelCell);
  
        for (let i = 0; i < colCountEmpty; i++) {
          const emptyCell = document.createElement('td');
          row.appendChild(emptyCell);
        }
  
        const valueCell = document.createElement('td');
        if (rowData.isBold) {
          const boldElement = document.createElement('b');
          boldElement.textContent = rowData.value;
          valueCell.appendChild(boldElement);
        } else {
          valueCell.textContent = rowData.value;
        }
        row.appendChild(valueCell);
      }
      tableBody.appendChild(row);
    });
  
    return tableBody;
  }

  function generateSummaryRows2(rowsData,cgst, sgst) {
    const tableBody = document.createElement('tbody');
  
    rowsData.forEach(rowData => {
      const row = document.createElement('tr');
      if (rowData.isBlank) {
        const cell = document.createElement('td');
        cell.setAttribute('colspan', rowData.colspan);
        row.appendChild(cell);
      } else {
        const labelCell = document.createElement('td');
        labelCell.textContent = rowData.label;
        row.appendChild(labelCell);
  
        for (let i = 0; i < 2; i++) {
          const emptyCell = document.createElement('td');
          row.appendChild(emptyCell);
        }

        const emptyCell1 = document.createElement('td');
        emptyCell1.innerHTML = cgst;
        row.appendChild(emptyCell1);

        for (let i = 0; i < 1; i++) {
            const emptyCell = document.createElement('td');
            row.appendChild(emptyCell);
        }

        const emptyCell2 = document.createElement('td');
        emptyCell2.innerHTML = sgst
        row.appendChild(emptyCell2);

        const valueCell = document.createElement('td');
        if (rowData.isBold) {
          const boldElement = document.createElement('b');
          boldElement.textContent = rowData.value;
          valueCell.appendChild(boldElement);
        } else {
          valueCell.textContent = rowData.value;
        }
        row.appendChild(valueCell);
      }
      tableBody.appendChild(row);
    });
  
    return tableBody;
  }


async function generateInvoice(invoiceData) {
    try {
        const response = await fetch('bill.html');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const htmlTemplate = await response.text();

        const parser = new DOMParser();
        const doc = parser.parseFromString(htmlTemplate, 'text/html');

        // Company Information
        const addressElement = doc.getElementById('Address');
        if (addressElement) addressElement.textContent = invoiceData.company.address;
        const gstNoElement = doc.getElementById('GstNo');
        if (gstNoElement) gstNoElement.textContent = invoiceData.company.gstNo;
        const emailElement = doc.getElementById('Email');
        if (emailElement) emailElement.textContent = invoiceData.company.email;


        // Delivery Note Details
        const invoiceNoElement = doc.getElementById('InvoiceNo');
        if (invoiceNoElement) invoiceNoElement.textContent = invoiceData.details.invoiceNo;
        const datedElement = doc.getElementById('Dated');
        if (datedElement) datedElement.textContent = invoiceData.details.dated;
        const deliveryNoteElement = doc.getElementById('DeliveryNote');
        if (deliveryNoteElement) deliveryNoteElement.textContent = invoiceData.details.deliveryNote;
        const refNoDateElement = doc.getElementById('RefNoDate');
        if (refNoDateElement) refNoDateElement.textContent = invoiceData.details.refNoDate;
        const otherRefElement = doc.getElementById('OtherRef');
        if (otherRefElement) otherRefElement.textContent = invoiceData.details.otherRef;
        const checkedByElement = doc.getElementById('CheckedBy');
        if (checkedByElement) checkedByElement.textContent = invoiceData.details.checkedBy;


        // Consignee and Buyer Details
        const cAddress1Element = doc.getElementById('Caddress1');
        if (cAddress1Element) cAddress1Element.textContent = invoiceData.consignee.address;
        const cContact1Element = doc.getElementById('Ccontact1');
        if (cContact1Element) cContact1Element.textContent = invoiceData.consignee.contactNo;
        const cAddress2Element = doc.getElementById('Caddress2');
        if (cAddress2Element) cAddress2Element.textContent = invoiceData.buyer.address;
        const cContact2Element = doc.getElementById('Ccontact2');
        if (cContact2Element) cContact2Element.textContent = invoiceData.buyer.contactNo;


        // Item Details
        const productTableBody = doc.getElementById('productTableEditable');
        if (productTableBody) {
             // Clear existing placeholder rows
             productTableBody.innerHTML = '';
             invoiceData.items.forEach(item => {
               const row = doc.createElement('tr');
               row.innerHTML = `
                 <td>${item.sNo}</td>
                 <td>${item.description}</td>
                 <td>${item.hsnSac}</td>
                 <td>${item.partNo}</td>
                 <td>${item.quantityShipped}</td>
                 <td>${item.quantityBilled}</td>
                 <td>${item.rate.toFixed(2)}</td>
                 <td>${item.discount}</td>
                 <td>${item.amount.toFixed(2)}</td>
               `;
               productTableBody.appendChild(row);
             });
        }

        // Example of how to use the function with parameter values:
        const subTotalValue = invoiceData.totals.subTotal;
        const cgstValue = invoiceData.totals.cgstAmount;
        const sgstValue = invoiceData.totals.sgstAmount;
        const discountValue = invoiceData.totals.specialDiscount;
        const roundOffValue = invoiceData.totals.roundOff;
        const totalValue = invoiceData.totals.total;
        
        
        const rowsData = [
            { colspan: 9, isBlank: true },
            { label: 'Sub Total', value: subTotalValue.toFixed(2) },
            { label: 'Output CGST', value: cgstValue.toFixed(2) },
            { label: 'Output SGST', value: sgstValue.toFixed(2) },
            { label: 'Special Discount', value: `(-)${discountValue.toFixed(2)}`, isBold: true },
            { label: 'Round Off', value: `(-)${roundOffValue.toFixed(2)}`, isBold: true },
            { label: 'Total', value: totalValue.toFixed(2) },
        ];
        
        const summaryRows = generateSummaryRows(rowsData, 6);
        productTableBody.appendChild(summaryRows);


        // GST Details
        const gstTableBody = doc.getElementById('gstTableEditable');
        if (gstTableBody) {
            // Clear existing placeholder rows
            gstTableBody.innerHTML = '';
            invoiceData.gstDetails.forEach(gst => {
                const row = doc.createElement('tr');
                row.innerHTML = `
                    <td>${gst.hsnSac}</td>
                    <td>${gst.taxableValue.toFixed(2)}</td>
                    <td>${gst.cgstRate}</td>
                    <td>${gst.cgstAmount.toFixed(2)}</td>
                    <td>${gst.sgstUtgstRate}</td>
                    <td>${gst.sgstUtgstAmount.toFixed(2)}</td>
                    <td>${gst.totalTaxAmount.toFixed(2)}</td>
                `;
                gstTableBody.appendChild(row);
            });
        }

        const rowsData2 = [
            { colspan: 8, isBlank: true },
            { label: 'Total', value: totalValue.toFixed(2) },
        ];

        const summaryRows2 = generateSummaryRows2(rowsData2, invoiceData.totals.cgstAmount.toFixed(2), invoiceData.totals.sgstAmount.toFixed(2));
        gstTableBody.appendChild(summaryRows2);

        const taxAmountInWordsElement = doc.getElementById('taxAmountInWords');
        if (taxAmountInWordsElement && invoiceData.amountsInWords) {
            taxAmountInWordsElement.textContent = invoiceData.amountsInWords.taxAmount;
        }

        const amountChargeableInWords = doc.getElementById('AmountChargeableInWords');
        if (amountChargeableInWords && invoiceData.amountsInWords) {
            amountChargeableInWords.textContent = invoiceData.amountsInWords.amountChargeable;
        }

        return doc.documentElement.outerHTML;
    } catch (error) {
        console.error('Error generating invoice:', error);
        return null;
    }
}



async function start() {

    var myInvoiceData = {
        company: {
            address: 'Your Company Address, City, Postal Code',
            gstNo: 'YOUR_GST_NUMBER',
            email: 'your.email@example.com'
        },
        details: {
            invoiceNo: 'INV-2023-001',
            dated: '2023-10-27',
            deliveryNote: 'DN-2023-005',
            refNoDate: 'REF-12345 / 2023-10-26',
            otherRef: 'Some other reference',
            checkedBy: 'John Doe'
        },
        consignee: {
            address: 'Consignee Address, City, Postal Code',
            contactNo: '+1 123 456 7890'
        }, 
        buyer: {
            address: 'Buyer Address, City, Postal Code',
            contactNo: '+1 987 654 3210'
        },
        items: [
          { sNo: 1, description: 'Nunbell Milk Bottle', hsnSac: '39233090', partNo: 'ACO38', quantityShipped: '10 No', quantityBilled: '10 No', rate: 210.00, discount: '50 %', amount: 1050.00 },
          { sNo: 2, description: 'Another Product', hsnSac: 'ABCD', partNo: 'XYZ', quantityShipped: '5 Each', quantityBilled: '5 Each', rate: 50.00, discount: '10 %', amount: 225.00 },
        ],

        // You would typically calculate these totals based on items
        totals: {
            subTotal: 1275.00, // Example calculated value
            cgstAmount: 114.75, // Example calculated value (9% of 1275)
            sgstAmount: 114.75, // Example calculated value (9% of 1275)
            specialDiscount: 50.00, // Example value
            roundOff: 0.00, // Example value
            total: 1454.50 // Example calculated value
        },
          gstDetails: [
            { hsnSac: '39233090', taxableValue: 1050.00, cgstRate: '9%', cgstAmount: 94.50, sgstUtgstRate: '9%', sgstUtgstAmount: 94.50, totalTaxAmount: 189.00 },
            { hsnSac: 'ABCD', taxableValue: 225.00, cgstRate: '9%', cgstAmount: 20.25, sgstUtgstRate: '9%', sgstUtgstAmount: 20.25, totalTaxAmount: 40.50 },
        ],
        amountsInWords: {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
            amountChargeable: 'INR One Thousand Four Hundred Fifty Four and Fifty Paise Only',
            taxAmount: 'INR Two Hundred Twenty Nine and Fifty Paise Only'
        }
    };

    const generatedHtml = await generateInvoice(myInvoiceData);

    document.getElementById("body").innerHTML = generatedHtml;


    
    // if (generatedHtml) {
    //     // Open in a new tab
    //     const newWindow = window.open('', '_blank');
    //     if (newWindow) {
    //         // Write the generated HTML to the new window
    //         newWindow.document.write(generatedHtml);
    //         newWindow.document.close(); // Important to signal the end of the document

    //         // Wait for the content to load before printing
    //         newWindow.onload = function() {
    //             // Trigger the print dialog
    //             newWindow.print();
    //         };
    //     } else {
    //         alert('Could not open new window. Please allow pop-ups for this site.');
    //     }
    // } else {
    //     console.error('Failed to generate invoice.');
    // }


}




start();