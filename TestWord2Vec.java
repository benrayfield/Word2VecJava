
import static org.junit.Assert.assertEquals;
import static benrayfield.util.X.*;
import  com.medallia.word2vec.Word2VecTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
//import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.medallia.word2vec.Word2VecModel;
import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Searcher.Match;
import com.medallia.word2vec.SearcherImpl;
//import com.medallia.word2vec.Searcher.UnknownWordException;
import com.medallia.word2vec.neuralnetwork.NeuralNetworkType;

public class TestWord2Vec{
	
	public static double[] minus(double[] a, double[] b){
		if(a.length != b.length) throw X("diff lengths");
		double[] ret = new double[a.length];
		for(int i=0; i<a.length; i++) ret[i] = a[i]-b[i];
		return ret;
	}
	
	/*public static double squaredRadius(double[] a){
		double ret = 0;
		for(int i=0; i<a.length; i++) ret += a[i]*a[i];
		return ret;
	}*/
	
	public static double radius(double[] a){
		//return Math.sqrt(squaredRadius(a));
		return Math.sqrt(dotProd(a,a));
	}
	
	/** Example: dotProduct of 2 vectors of radius x and y ranges plus/minus x*y */
	public static double dotProd(double[] a, double[] b){
		double ret = 0;
		for(int i=0; i<a.length; i++) ret += a[i]*b[i];
		return ret;
	}
	
	/** range -1 to 1. Same result, except roundoff, if swap the 2 params. */
	public static double cosineSimilarity(double[] a, double[] b){
		if(a.length != b.length) throw X("diff lengths");
		double aRadius = radius(a), bRadius = radius(b); //TODO optimize. Could do dotProd and radiuses together faster.
		double divide = aRadius*bRadius;
		if(divide == 0) return 0; //both vectors are all 0s (or close to it and roundoff says 0), so its equally true that they're opposite vs same direction
		double ret = dotProd(a,b)/divide;
		ret = Math.max(0, Math.min(ret, 1)); //in case roundoff makes it slightly less than -1 or slightly more than 1
		return ret;
	}
	
	/*public static double sumOfSquares(double[] a, double[] b){
		if(a.length != b.length) throw X("diff lengths");
		double ret = 0;
		for(int i=0; i<a.length; i++){
			double diff = a[i]-b[i];
			ret += diff*diff;
		}
		return ret;
	}
	
	public static double distance(double[] a, double[] b){
		return sumOfSquare(a,b);
		return 
		if(a.length != b.length) throw X("diff lengths");
		double[] ret = new double[a.length];
		for(int i=0; i<a.length; i++) ret[i] = a[i]-b[i];
		return ret;
	}*/
	
	/** like Double.compare except doesnt check for NaN or negative 0 etc */
	public static int compare(double x, double y){
		if(x < y) return -1;
		if(x > y) return 1;
		return 0;
	}
	
	public static Comparator<String> compareBySimilarityTo(double[] vec, Map<String,double[]> wordToVec){
		return (String wordX, String wordY)->{
			double[] x = wordToVec.get(wordX);
			double[] y = wordToVec.get(wordY);
			return compare(cosineSimilarity(vec,x), cosineSimilarity(vec,y));
		};
	}

	/** some of this test code copied from com.medallia.word2vec.Word2VecTest.testGetWordByVector and modified */
	public static void main(String[] args){	
		try{
			Word2VecModel model = Word2VecTest.trainer()
				.type(NeuralNetworkType.SKIP_GRAM)
				.train(Word2VecTest.testData());
			
			SearcherImpl searcher = (SearcherImpl)model.forSearch(); //cuz need getVectorOrNull
			/*double[] vectorOfAnarchism = searcher.getVectorOrNull("anarchism");
			double[] vectorOfTrouble = searcher.getVectorOrNull("trouble");
			double[] vectorOfAround = searcher.getVectorOrNull("around");
			*/
			NavigableMap<String,double[]> wordToVec = new TreeMap();
			for(String word : model.getVocab()){
				wordToVec.put(word, searcher.getVector(word));
			}
			double[] anarchism = wordToVec.get("nine");
			//double[] anarchism = wordToVec.get("nine");
			List<String> words = new ArrayList(wordToVec.keySet());
			Collections.sort(words, compareBySimilarityTo(anarchism, wordToVec).reversed());
			for(int i=0; i<100; i++){
				String wordStr = words.get(i);
				double[] wordVec = wordToVec.get(wordStr);
				System.out.println(wordStr+" "+cosineSimilarity(anarchism,wordVec));
			}
			
			System.out.println("\r\n\r\n\r\n\r\n----------\r\n\r\n");
			for(Match match : searcher.getMatches("nine",100)){
				String wordStr = match.match();
				double num = match.distance();
				System.out.println(match.match()+" "+num);
			}
			
			/** The numbers match except roundoff. So I've verified how its comparing them, but havent yet verified how it generates the vectors (2020-9-24).
			
			This implementation:
			nine 1.0
			on 0.9511848770507607
			and 0.950323138577
			four 0.9465786006696192
			six 0.942981090893018
			alabama 0.9418681849987058
			in 0.9404513753752977
			seven 0.9324483863920099
			two 0.9231282077947134
			eight 0.9112816342218029
			dhabi 0.8788868714759794
			achilles 0.8709594793517783
			abu 0.8650106144161235
			temperature 0.8631547380196368
			american 0.8630746747711977
			radiation 0.8588134465925792
			of 0.8587329222738909
			one 0.8585463730977422
			three 0.8540913136258741
			
			Searcher.getMatches:
			nine 0.9999999999999998
			on 0.9511848770507605
			and 0.9503231385769997
			four 0.9465786006696191
			six 0.9429810908930178
			alabama 0.9418681849987056
			in 0.9404513753752977
			seven 0.9324483863920097
			two 0.9231282077947133
			eight 0.9112816342218027
			dhabi 0.8788868714759792
			achilles 0.8709594793517782
			abu 0.8650106144161233
			temperature 0.8631547380196367
			american 0.8630746747711975
		*/
			
			
			/** It works. The testcase that came with word2vec said it should be "anarchism", "feminism", "trouble", "left", "capitalism",
			as in[
				// This vector defines the word "anarchism" in the given model.
				double[] vectors = new double[] { 0.11410251703652753, 0.271180824514185, 0.03748515103121994, 0.20888126888511183, 0.009713531343874777, 0.4769425625416319, 0.1431890482445165, -0.1917578875330224, -0.33532561802423366,
					-0.08794543238607992, 0.20404593606213406, 0.26170074241479385, 0.10020961212561065, 0.11400571893146201, -0.07846426915175395, -0.19404092647187385, 0.13381991303455204, -4.6749635342694615E-4, -0.0820905789076496,
					-0.30157145455251866, 0.3652037905836543, -0.16466827556950117, -0.012965932276668056, 0.09896568721267748, -0.01925755122093615 };
	
				List<Match> matches = model.forSearch().getMatches(vectors, 5);
	
				assertEquals(
					ImmutableList.of("anarchism", "feminism", "trouble", "left", "capitalism"),
					Lists.transform(matches, Match.TO_WORD)
				);
			]
			
			OUTPUT:
			Stage ACQUIRE_VOCAB, progress 0.0%
			Stage FILTER_SORT_VOCAB, progress 0.0%
			Stage CREATE_HUFFMAN_ENCODING, progress 0.0%
			Stage CREATE_HUFFMAN_ENCODING, progress 0.5%
			Stage TRAIN_NEURAL_NETWORK, progress 0.0%
			Stage TRAIN_NEURAL_NETWORK, progress 0.7580901473562555%
			anarchism 1.0
			feminism 0.9951183484715262
			trouble 0.995090673025493
			left 0.9944657159413355
			capitalism 0.9944625519682271
			uk 0.9943508838662302
			christian 0.9942897523672425
			goldman 0.9942382402315917
			anarchy 0.9942240399133522
			post 0.9941859816247111
			links 0.9940705281688388
			anarcho 0.9939376269065783
			anarchists 0.9939335530180733
			own 0.9937789073716125
			violence 0.9934578616262572
			called 0.9932717304766873
			tolstoy 0.9932632675011989
			movement 0.9932577419643163
			make 0.9932173288051879
			might 0.9931515039407549
			natural 0.993090611698435
			anarcha 0.9929470899269419
			specific 0.9929457517819438
			different 0.992812396473863
			work 0.9927999400122997
			against 0.9927588885901887
			principles 0.992744384333294
			religious 0.9927323449600269
			activity 0.992653578921484
			treatment 0.9926458670632298
			views 0.9926454812876966
			ideas 0.9926439636483585
			associated 0.9926311816683514
			meaning 0.992616993193979
			environment 0.9925059468545251
			structures 0.9925037690866414
			black 0.9924703596215347
			thought 0.9924422185694127
			ability 0.9923638565662043
			communism 0.9922760452903735
			theory 0.9921729582665767
			within 0.9921254450036363
			communities 0.9920891068769175
			individualist 0.992088972569432
			able 0.992048429347299
			free 0.9920472345121145
			institutions 0.9920031093040648
			considered 0.9919769300697211
			intellectual 0.9919476534668712
			violent 0.9919315554873582
			liberty 0.9919286755925912
			further 0.9918192223297816
			single 0.99169468920453
			god 0.9915328850613743
			libertarian 0.9914568296892267
			still 0.9913428278455858
			without 0.9913385245760651
			particular 0.9912074836748956
			believe 0.9912029449072212
			communicate 0.9911991121874842
			become 0.9911147956403134
			general 0.9910934885251881
			take 0.9910810871516175
			should 0.9910677836468863
			authority 0.9910596782195001
			revolutionary 0.9910405935582746
			argue 0.9909303413157149
			nature 0.9909117920302792
			way 0.9908300603150529
			similar 0.9907812523023952
			kropotkin 0.990695145459413
			voting 0.9906777478807974
			since 0.9905885873656336
			authoritarian 0.9905579189837684
			long 0.9905403154122849
			bakunin 0.9904443834004887
			communist 0.9904001554134011
			community 0.9903425982054792
			day 0.9902858834264929
			syndicalism 0.9902820051389777
			de 0.9902266449085216
			need 0.9902148088107882
			trade 0.9902106074027524
			economic 0.9901710531607489
			typical 0.9901027865327814
			europe 0.9899863847317975
			will 0.9899607460011296
			labour 0.989872209400294
			mean 0.9898482256568942
			workers 0.9898270223539359
			list 0.9897556492151524
			others 0.9897542785739352
			living 0.9897262235708565
			related 0.9895994182375856
			late 0.989522274092999
			society 0.9894874189805435
			school 0.9894798224799591
			self 0.9893853487435808
			research 0.9893710234235572
			communists 0.9893464037869056
			*/

			
			
		}catch(Exception e){ throw X(e); }
	}

}
