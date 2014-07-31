package nav.script;

@SuppressWarnings("serial") class ScriptRaiseException extends RuntimeException {
   final String raised;

   public ScriptRaiseException(String raised) {
      super("RAISED: " + raised);

      this.raised = raised;
   }

   public String getRaised() {
      return raised;
   }
}