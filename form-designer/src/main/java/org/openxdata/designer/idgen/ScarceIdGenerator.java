package org.openxdata.designer.idgen;

public interface ScarceIdGenerator extends IdGenerator {

	void makeIdAvailable(int id);

	void reserveId(int id);
}
