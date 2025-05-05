


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
