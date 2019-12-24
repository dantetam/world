package io.github.dantetam.world.process.customproc;

import java.util.List;
import java.util.Map;

import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.process.LocalProcess;

public class CaravanTradeProcess extends LocalProcess {

	public CaravanTradeProcess(String name, Society host, Society target, 
			Map<Integer, Integer> resourceExchangeResult) {
		super(name, null, null, null, false, null, null, null, 1, null);
		// Auto-generated constructor stub
		//TODO; Add special properties to the caravan trade process 
		//As well as a skill process distribution for social/barter trading skills for trading between societies/people
	}
	
}
