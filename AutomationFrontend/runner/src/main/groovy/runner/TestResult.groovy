package runner

/**
 * Resultado de ejecución de un test case.
 */
class TestResult {
    String name
    String status       // PASSED | FAILED | ERROR | SKIPPED
    String errorMessage // null si PASSED
    long   durationMs   // tiempo de ejecución

    boolean isPassed()  { status == 'PASSED'  }
    boolean isFailed()  { status == 'FAILED'  }
    boolean isError()   { status == 'ERROR'   }
    boolean isSkipped() { status == 'SKIPPED' }
}
