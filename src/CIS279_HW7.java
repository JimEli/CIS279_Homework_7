/*************************************************************************
 * Title: Loan Eligibility Calculator
 * File: EliJames_HW7.java
 * Author: James Eli
 * Date: 2/1/2017
 *
 * This JavaFX program calculates loan eligibility determined by monthly 
 * income being within a percentage of the monthly loan payment. The 
 * threshold percentage for eligibility is defined by the value:
 * MAXIMUM_INCOME_TO_LOAN_PERCENTAGE, this value can be changed if needed.
 *
 * This version of the program changes the background color of the input 
 * textfields whenever non-numeric characters are present. Furthermore,
 * this version of the program adds combo-boxes for input of the annual 
 * interest rate and loan term. The values for these combo-boxes are read 
 * from files.
 * 
 * Notes: 
 *   (1) Compiled with java SE JDK 8, Update 121 (JDK 8u121) and JavaFX
 *   version 8.0.121-b13.
 *   
 * Submitted in partial fulfillment of the requirements of PCC CIS-279.
 *************************************************************************
 * Change Log:
 *   02/01/2017: Initial release. JME
 *   04/12/2017: Truncated loan-wage percentage. JME
 *               Changed regex match to correctly handle $USD amounts. JME
 *   07/22/2017: Added style sheet (textfield.css) functionality. JME
 *************************************************************************/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CIS279_HW7 extends Application {
  // Maximum monthly income-to-loan payment percentage. Exceeding this percentage means not eligible for loan.  
  public static final double MAXIMUM_INCOME_TO_LOAN_PERCENTAGE = 25.0;

  // Textfield array indexes.
  //private static final int SALARY_INCOME = 0;
  //private static final int INTEREST_INCOME = 1;
  //private static final int INVESTMENT_INCOME = 2;
  //private static final int OTHER_INCOME = 3;
  private static final int LOAN_AMOUNT = 4;
  //private static final int ANNUAL_INTEREST_RATE = 0;
  //private static final int TERM_IN_YEARS = 1;
  private static final int TOTAL_INCOME = 0;
  private static final int MONTHLY_PAYMENT = 1;
  private static final int TOTAL_PAYMENT = 2;

  // Minimum and maximum input values.
  public static final double MINIMUM_INCOME_INPUT = 0.0d;
  public static final double MAXIMUM_INCOME_INPUT = 1000000.0d;
  public static final double MINIMUM_LOAN_AMOUNT = 0.0d;
  public static final double MAXIMUM_LOAN_AMOUNT = 1000000.0d;
  
  // Textfield tooltips and dialog texts.
  private final String[] ttText = new String[] {
    "Annual salary and wages must be a \ndollar amount between $0 and 1M\n",
    "Annual interest income mustbe a \ndollar amount between $0 and 1M\n",
    "Annual investment income must be a \ndollar amount between $0 and 1M\n",
    "Annual other income must be a \ndollar amount between $0 and 1M\n",
    "Loan amount must be a dollar \namount between $0 and 1M\n"
  };
  
  // 5 textfields used as inputs.
  private final TextField[] tfInputs = new TextField[5];
  // 3 textfields used as outputs.
  private final TextField[] tfOutputs = new TextField[3];

  // Loan eligibility text.
  private final Text txtEligibility = new Text();

  // Output states, set to true when output has been displayed.
  private static boolean incomeOutput = false;
  private static boolean loanOutput = false;
  
  /**********************************
   * JavaFX application start method. 
   *********************************/
  @Override
  public void start( Stage primaryStage ) {
    // Create ArrayList of Strings used to temporarily hold text file data.
    final ArrayList<String> terms = new ArrayList<String>();
    final ArrayList<String> rates = new ArrayList<String>();

    // Double/Integer object properties hold loan rate & term data for binding to comboboxes.
    final ObjectProperty<Double> loanRate = new SimpleObjectProperty<Double>( 0.0d );
    final ObjectProperty<Integer> loanTerm = new SimpleObjectProperty<Integer>( 0 );
    
    // Input and output label texts.
    final String[] tfInLabels = new String[] { 
      "Salary and Wages:", "Interest Income:", "Investment Income:", "Other Income:", "Loan Amount:" };
    final String[] tfOutLabels = new String[] { 
      "Total Income:", "Monthly Payment:", "Total Payments over Life of Loan:" };
    
    // 5 tooltips for input textboxes.
    final Tooltip[] tooltip = new Tooltip[5];
    
    // Grid pane column and row positions for textfields and labels.
    final int[] inColumns = new int[] { 1, 1, 1, 1, 3 };
    final int[] inRows = new int[] { 0, 1, 2, 3, 2 };
    final int[] outColumns = new int[] { 1, 3, 3 };
    final int[] outRows = new int[] { 4, 3, 4 };
    
    // 2 buttons.
    final Button btCalculate = new Button( "Calc Payment" );
    final Button btCancel = new Button( "Cancel" );

    // Style changes based upon default modena.css
    setUserAgentStylesheet( STYLESHEET_MODENA );
    
    // Create UI.
    GridPane gridPane = new GridPane();
    gridPane.setAlignment( Pos.CENTER );
    gridPane.setHgap( 5 );
    gridPane.setVgap( 5 );

    // Setup output textfields.
    for ( int i=0; i<3; i++ ) {
      tfOutputs[i] = new TextField();
      gridPane.add( tfOutputs[i], outColumns[i], outRows[i] );
      gridPane.add( new Label( tfOutLabels[i] ), outColumns[i] - 1, outRows[i] );
      tfOutputs[i].setAlignment( Pos.BOTTOM_RIGHT );
      tfOutputs[i].setEditable( false );         // Not editable.
      tfOutputs[i].setFocusTraversable( false ); // Cannot receive focus.
    }

    // Setup input textfields.
    for ( int i=0; i<5; i++ ) {
      tfInputs[i] = new TextField();
      if ( i != LOAN_AMOUNT )
        gridPane.add( tfInputs[i], inColumns[i], inRows[i] );
      gridPane.add( new Label( tfInLabels[i] ), inColumns[i] - 1, inRows[i] );
      tfInputs[i].setAlignment( Pos.BOTTOM_RIGHT );

      // Listeners clear output textfields when new data is entered. 
      // Thus eliminating incorrect/stale data.  
      if ( i < 4 ) {
    	// Handle income textfields.
        tfInputs[i].textProperty().addListener( ( observable, oldValue, newValue ) -> {
          validateTextField( (TextField)((StringProperty)observable).getBean() );
          // Check output state.
          if ( incomeOutput ) {
            tfOutputs[TOTAL_INCOME].setText( "" );
            txtEligibility.setText( "" ); // Remove stale data.
            incomeOutput = false; // Reset state.
          }
        } );
      } else {
    	// Handle loan amount textfield.
        tfInputs[i].textProperty().addListener( ( observable, oldValue, newValue ) -> {
          validateTextField( (TextField)((StringProperty)observable).getBean() );
          if ( loanOutput ) { 
            clearLoanOutputs(); // Remove stale data.
            loanOutput = false; // Reset state.
          }
        } );
      }  

      // Add tooltips for input textfields.
      tooltip[i] = new Tooltip();
      tooltip[i].setText( ttText[i] );
      tfInputs[i].setTooltip( tooltip[i] );
      // Set id for textfields. Used to identify the textfield when an exception is thrown.
      tfInputs[i].setId( Integer.toString(i) );
    }

    // Setup combo-boxes.
    final ComboBox<Double> cboLoanRate = new ComboBox<>(); 
    gridPane.add( new Label( "Annual Interest Rate (n.nn%):" ), 2, 0 );
    readFileItems( "annual_interest_rates.txt", rates );
    for ( String s : rates ) {
      try {
    	cboLoanRate.getItems().add( Double.parseDouble( s ) );
      } catch ( Exception e ) {
        // Throw up an error dialog and terminate program.
        alertDialog( "File Data Error", "Annual Interest Rate ComboBox data file corrupted!" );
        Platform.exit();
      }
    }
    gridPane.add( cboLoanRate, 3, 0 );
    cboLoanRate.valueProperty().bindBidirectional( loanRate );
    cboLoanRate.getSelectionModel().selectFirst();
    loanRate.addListener( observable -> { clearLoanOutputs(); } );

    final ComboBox<Integer> cboLoanTerm = new ComboBox<>(); 
    gridPane.add( new Label( "Term in Years:" ), 2, 1 );
    readFileItems( "loan_terms.txt", terms );
    for ( String s : terms ) {
      try {
        cboLoanTerm.getItems().add( Integer.parseInt( s ) );
      } catch ( Exception e ) {
        // Throw up an error dialog and terminate program.
        alertDialog( "File Data Error", "Loan Term ComboBox data file corrupted!" );
        Platform.exit();
      }
    }
    gridPane.add( cboLoanTerm, 3, 1 );
    cboLoanTerm.valueProperty().bindBidirectional( loanTerm );
    cboLoanTerm.getSelectionModel().selectFirst();
    loanTerm.addListener( observable -> { 
      clearLoanOutputs(); // Remove stale data.
      loanOutput = false; // Reset state.
    } );

    // Placed here to set proper focus/tab order, following the comboboxes. 
    // AFAIK, focus/tab order is only based upon when object is inserted into GridPane.
    gridPane.add( tfInputs[4], 3, 2 );

    // Set area for loan eligibility text, spanning gridpane column 2 and 3 to fit.
    gridPane.add( txtEligibility, 2, 5, 3, 5 );

    // Set buttons.
    gridPane.add( btCalculate, 0, 5 );
    gridPane.add( btCancel, 1, 5 );
    // Position buttons.
    GridPane.setHalignment( btCalculate, HPos.LEFT );
    GridPane.setHalignment( btCancel, HPos.LEFT );
    // Process button events.
    btCancel.setOnAction( e -> Platform.exit() );
    btCalculate.setOnAction( e -> calculateLoanPayment( loanRate.getValue(), loanTerm.getValue() ) );
    
    // Create the scene and place it in the stage
    Scene scene = new Scene( gridPane, 650, 200 );

    // Attempt to load our textfield css.
    try { 
      scene.getStylesheets().add( getClass().getClassLoader().getResource( "textfield.css" ).toString() );
    } catch ( Exception e ) {
      // Throw up an error dialog and terminate program.
      alertDialog( "File Data Error", "textfield.css file missing or corrupted!" );
      Platform.exit();
    }

    // Set window title.
    primaryStage.setTitle( "Loan Payment and Eligibility Calculator Form" );
    primaryStage.setScene( scene ); // Place the scene in the stage.
    primaryStage.show();            // Display the stage.

  }
  
  /***************************************************************
   * Clear loan output textfields.
   **************************************************************/
  private void clearLoanOutputs() {
    tfOutputs[MONTHLY_PAYMENT].setText( "" );
    tfOutputs[TOTAL_PAYMENT].setText( "" );
    txtEligibility.setText( "" );
  }

  /***************************************************************
   * Validate textfield characters.
   **************************************************************/
  public void validateTextField( TextField tf ) {
    // textfield is valid when it is empty or if contains only digits and/or decimal.
    if ( tf.getText().isEmpty() || tf.getText().matches( "(\\d{1,3}(\\,\\d{3})*|\\d*)(\\.)?(\\d{0,2})?" ) )   
      tf.getStyleClass().remove( "error" );  // Remove red background.
    else  
      tf.getStyleClass().add( "error" );     // Add red background.
  }

  /*****************************************************************
   * Attempt to convert textfield input from String to double value.
   ****************************************************************/
  private double getDoubleFromTextField( TextField tf ) throws IllegalArgumentException {
    if ( tf.getLength() != 0 && tf.getText() != null ) { 
      if ( tf.getText().matches( "(\\d{1,3}(\\,\\d{3})*|\\d*)(\\.)?(\\d{0,2})?" ) )  
        return Double.parseDouble( tf.getText().toString() );
      else
        // Throw iae and pass index of textfield.
        throw new IllegalArgumentException( tf.getId() );
    }
    // An empty textfield is returned as 0.
    return 0.0d;
  }
  
  /*****************************************
   * Event handler for the calculate button.
   ****************************************/
  private void calculateLoanPayment( double rate, int term ) {
    double totalIncome = 0.0d; // Total of all forms of income.
    double loanAmount = 0.0;   // Loan amount.
    double d;                  // Temporary holder for input values.
    
    try {
      // Attempt to retrieve inputs and validate. Failed validation throws an iae 
      // and our catch block then displays an error dialog.
      // Loop through all income inputs.
      for ( int i=0; i<4; i++ ) {
        d = getDoubleFromTextField( tfInputs[i] );
        if ( d < MINIMUM_INCOME_INPUT || d > MAXIMUM_INCOME_INPUT )
          throw new IllegalArgumentException( String.valueOf(i) );
        totalIncome += d;
      }
      tfOutputs[TOTAL_INCOME].setText( String.format( "$%.2f", totalIncome ) );
      incomeOutput = true; // Set output state.
       
      // Loan amount input.
      d = getDoubleFromTextField( tfInputs[LOAN_AMOUNT] );
      if ( d < MINIMUM_LOAN_AMOUNT || d > MAXIMUM_LOAN_AMOUNT )
        throw new IllegalArgumentException( String.valueOf(LOAN_AMOUNT) );
      loanAmount = d;

      // Create a loan object. Loan defined in Textbook Listing 10.2
      Loan loan = new Loan( rate, term, loanAmount );

      // Display monthly payment and total payment
      tfOutputs[MONTHLY_PAYMENT].setText( String.format( "$%.2f", loan.getMonthlyPayment() ) );
      tfOutputs[TOTAL_PAYMENT].setText( String.format( "$%.2f", loan.getTotalPayment() ) );
      loanOutput = true; // Set output state.

      // Check if loan payment is within income-to-loan percentage limit.
      double percentage = loan.getMonthlyPayment()/(totalIncome/1200.);
      // Truncate result to 1 decimal place.
      percentage = new BigDecimal( String.valueOf( percentage ) ).setScale( 1, BigDecimal.ROUND_FLOOR ).doubleValue();
      if ( percentage <= MAXIMUM_INCOME_TO_LOAN_PERCENTAGE ) { 
        txtEligibility.setFill( Color.GREEN );
        txtEligibility.setText( String.format( "Eligible for loan! Loan to income percentage: %2.1f%%", percentage ) );
      } else {
        txtEligibility.setFill( Color.RED );
        txtEligibility.setText( String.format( "Not Eligible for loan! Loan to income percentage: %2.1f%%", percentage ) );
      }

    } catch ( Exception e ) {
      // Catch the iae exception we've thrown?
      if ( e.toString() != null && e.toString().contains( "IllegalArgumentException" ) ) {
        int i = Integer.parseInt( e.getMessage() );
        // Throw up an error dialog.
        alertDialog( "Input Error", ttText[i] );
        // Set focus to textfield with input error.
        tfInputs[i].requestFocus();
      } else 
        // Throw up a generic error message.
        alertDialog( "Input Error", "There is a problem with your input!" );
    }
  }

  /***************************************************************
   * Display alert dialog with supplied text.
   **************************************************************/
  private void alertDialog( String title, String alertMessage ) {
    // Throw up an error dialog.
    Alert alert = new Alert( AlertType.ERROR );
    alert.setTitle( title );
    alert.setHeaderText( null );
    alert.setContentText( alertMessage );
    alert.showAndWait();
  }
  
  /***************************************************************
   * Read file data into a string list.
   **************************************************************/
  private void readFileItems( String fileName, ArrayList<String> list ) {
    // Attempt to read entire text file into an ArrayList of Strings.
    try ( BufferedReader br = new BufferedReader( new FileReader( fileName ) ) ) {
      String s; // Temporary String holds input line.
	      
      while ( ( s = br.readLine() ) != null )  
    	if ( s.trim().length() > 0 ) // Skip blank lines.
          list.add( s );
    // Catch various file system errors here.
    } catch( IOException | InvalidPathException e ) {
      System.err.println( "Cannot find file.\nEnsure data file is inside active directory." );
      return; // Exit.
    } catch( SecurityException e ) {
      System.err.println( "Cannot access file." );
      return; // Exit.
    } catch( Exception e ) {
      System.err.println( "Generic file error." );
      return; // Exit.
    }
  }
  
  public static void main( String[] args ) { launch( args ); }

}
