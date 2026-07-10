/**
 * Google Apps Script for Room Service - v4 (Separate Columns)
 *
 * Paste this into Extensions > Apps Script in your Google Sheet.
 * This version ensures Room Type, Room Unit, Sync Date, and Sync Time
 * are in their own SEPARATE COLUMNS for easy filtering.
 */

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var action = data.action;

    if (action === 'sync') {
      var booking = data.booking;
      return handleBookingSync(booking);
    }

    return ContentService.createTextOutput("Invalid Action").setMimeType(ContentService.MimeType.TEXT);

  } catch (error) {
    return ContentService.createTextOutput("Error: " + error.toString()).setMimeType(ContentService.MimeType.TEXT);
  }
}

function handleBookingSync(booking) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Reservations") || ss.insertSheet("Reservations");

  // Set Headers if sheet is empty
  // Header Row with separated columns
  if (sheet.getLastRow() === 0) {
    var headers = [
      "Booking Date", "Room Type", "Room Quantity", "Sync Date", "Sync Time", // Separate Date Info
      "Booking ID", "Guest Name", "Phone", "Room No",
      "Check-In", "Check-Out", "Total Amount", "Advance Paid",
      "Remaining", "Agent", "Payment Mode", "Status"
    ];
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#D1D5DB");
  }

  // Data Row with separated columns
  var rowData = [
    booking.bookingDate,       // Column A: Date of creation
    booking.roomType,          // Column B
    booking.roomQuantity,      // Column C
    booking.syncDate,          // Column D
    booking.syncTime,          // Column E
    booking.bookingNumber,
    booking.guestName,
    "'" + booking.guestPhone,
    booking.roomNumber,
    new Date(booking.checkInDate),
    new Date(booking.checkOutDate),
    booking.totalAmount,
    booking.advancePaid,
    booking.remainingAmount,
    booking.bookingAgent,
    booking.paymentMode,
    booking.status
  ];

  // Append as a new row (History Log)
  sheet.appendRow(rowData);

  return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
}
