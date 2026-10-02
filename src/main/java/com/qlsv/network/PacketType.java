package com.qlsv.network;

public enum PacketType {
    PING,           // Client pings Server to check connection
    PONG,           // Server responds to Ping
    CONNECT_DB,     // Client sends SQL credentials to Server
    DB_STATUS,      // Server returns DB connection success/failure
    ADD_STUDENT,    // Client sends a student row to Server
    STUDENT_RESULT, // Server returns calculated student result (Name, ID, Average)
    SEARCH_STUDENTS, // Client searches students by query
    UPDATE_STUDENT,  // Client updates existing student record
    DELETE_STUDENT,  // Client deletes student record
    GET_ALL_STUDENTS,// Client fetches all student records
    STUDENTS_LIST,   // Server returns array of student records
    ERROR           // Server returns error message
}
