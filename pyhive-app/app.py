from sqlalchemy import *
from sqlalchemy.engine import create_engine
from sqlalchemy.schema import *

engine = create_engine('hive://pace_aster@10.201.114.15:10000/pace_asterdb')
conn = engine.connect()



mins = Table('AI_MINS_NETWORK_BASH_V2', MetaData(bind=engine), autoload=True)

query = select([mins])

result = conn.execute(query)

print result.fetchone()
