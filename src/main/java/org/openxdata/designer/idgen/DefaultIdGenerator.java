package org.openxdata.designer.idgen;

import java.util.ArrayList;
import java.util.List;

public class DefaultIdGenerator implements ScarceIdGenerator {

	int prevId;
	int maxId;
	List<Integer> freeList = new ArrayList<Integer>();
	List<Integer> reserveList = new ArrayList<Integer>();

	public DefaultIdGenerator() {
		this(1, Integer.MAX_VALUE);
	}

	public DefaultIdGenerator(int startId, int maxId) {
		this.prevId = startId - 1;
		this.maxId = maxId;
	}

	public int nextId() {

		boolean freeIdsAvailable = !freeList.isEmpty();
		boolean canGenerateId = prevId < maxId;

		if (!freeIdsAvailable && !canGenerateId)
			throw new RuntimeException("Id list exhausted: no available ids");

		if (freeIdsAvailable) {
			this.prevId = freeList.remove(0);
			return this.prevId;
		}

		if (canGenerateId) {
			for (int candidateId = this.prevId + 1; candidateId < maxId; candidateId++) {
				if (reserveList.contains(candidateId))
					continue;
				else
					return prevId = candidateId;
			}
		}

		throw new Error("Implementation error, should never reach here.");
	}

	public void makeIdAvailable(int id) {
		reserveList.remove(Integer.valueOf(id));
		if (!freeList.contains(id))
			freeList.add(id);
	}

	public void reserveId(int id) {
		freeList.remove(Integer.valueOf(id));
		reserveList.add(id);
	}

}
