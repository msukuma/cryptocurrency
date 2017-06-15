package scroogeCoin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {
	private UTXOPool pool;
	double sumInputs;
	double sumOutputs;
	Set<UTXO> inputsToSpend;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // TODO
    	pool = utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	reset();
    	return areInputsValid(tx) && areOutputsValid(tx) && sumInputs >= sumOutputs; //(5)
    }
   

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	List<Transaction> validTransactions = new ArrayList<Transaction>();
        // TODO
    	for (Transaction tx : possibleTxs) {
    		if(isValidTx(tx)){
    			validTransactions.add(tx);
    			removeSpentOutputs(tx);
    			addNewOutputsToPool(tx);
    		}
		}

    	return validTransactions.toArray(new Transaction[validTransactions.size()]);
    }
    
    private void reset(){
    	sumInputs = 0;
    	sumOutputs = 0;
    	inputsToSpend = new HashSet<UTXO>();
    }
    
    private boolean isDoubleSpendAttempt(UTXO utx){
    	return inputsToSpend.contains(utx) || !pool.contains(utx); // (3) && (1)
    }
    
    private boolean isValidSignature(Transaction tx, Transaction.Input input, int inputIndex, Transaction.Output output){ // (2)
    	return Crypto.verifySignature(output.address, tx.getRawDataToSign(inputIndex), input.signature);
    }
  
    private boolean areInputsValid(Transaction tx){
    	List<Transaction.Input> inputs = tx.getInputs();

    	for (int index = 0; index < inputs.size(); index++) {
    		Transaction.Input input = inputs.get(index);
    		UTXO utx = new UTXO(input.prevTxHash, input.outputIndex);

    		if(isDoubleSpendAttempt(utx)){ 
    			return false;
    		}

    		Transaction.Output output = pool.getTxOutput(utx);

			if(!isValidSignature(tx, input, index, output)){ 
				return false;
			};

    		inputsToSpend.add(utx); //(3)
    		sumInputs += pool.getTxOutput(utx).value;
		}

    	return true;
    }

    private boolean areOutputsValid(Transaction tx){
    	List<Transaction.Output> outputs = tx.getOutputs();

    	for (Transaction.Output output : outputs) {
			if (output.value < 0) { //(4)
				return false;
			}
			sumOutputs += output.value;
		}

    	return true;
    }
    
    private void removeSpentOutputs(Transaction tx){
    	for(Transaction.Input input : tx.getInputs()){
			UTXO utx = new UTXO(input.prevTxHash, input.outputIndex);
			pool.removeUTXO(utx);
			System.out.println("hello");
		}
    }
    
    private void addNewOutputsToPool(Transaction tx){
    	for (int i = 0; i <  tx.getOutputs().size(); i++) {
			Transaction.Output output = tx.getOutputs().get(i);
			UTXO utx = new UTXO(tx.getHash(), i);
			pool.addUTXO(utx, output);
		}
    }

}
