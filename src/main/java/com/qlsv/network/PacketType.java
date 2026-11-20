package com.qlsv.network;

public enum PacketType {
    PING,               // Client pings Server (Heartbeat)
    PONG,               // Server responds to Ping
    CONNECT_DB,         // Client sends SQL credentials to Server
    DB_STATUS,          // Server returns DB connection success/failure
    LOGIN,              // Client sends Username/Password for Server authentication
    LOGIN_RESPONSE,     // Server returns auth token and user profile
    ADD_STUDENT,        // Client sends a student record
    UPDATE_STUDENT,     // Client updates an existing student record
    DELETE_STUDENT,     // Client deletes a student record
    STUDENT_RESULT,     // Server returns calculated student result
    SEARCH_STUDENTS,    // Client searches students by query/filters
    GET_ALL_STUDENTS,   // Client fetches all student records
    STUDENTS_LIST,      // Server returns array of student records
    GET_TRANSCRIPT,     // Client fetches full transcript with course grades
    TRANSCRIPT_RESULT,  // Server returns full transcript object
    IMPORT_EXCEL,       // Client sends list of students from Excel for bulk insert
    IMPORT_RESULT,      // Server returns number of records imported
    ACK,                // RUDP Acknowledgment packet
    ERROR               // Server returns error message
}
