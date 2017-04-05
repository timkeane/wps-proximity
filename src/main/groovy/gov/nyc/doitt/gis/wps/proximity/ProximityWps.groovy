package gov.nyc.doitt.gis.wps.proximity

import java.util.List

import geoscript.geom.Point
import geoscript.feature.*
import geoscript.layer.Layer

title = 'Proximity'
description = 'Proximity'

inputs = [
	point: [name: 'point', title: 'point', type: Point.class],
	layer: [name: 'layer', title: 'layer', type: Layer.class]
]

outputs = [
	result: [name: 'result', title: 'result', type: Layer.class]
]

def run(input){
	Layer inLayer = input.layer
	Point inPoint = input.point
	List fields = inLayer.schema.fields
	fields.add(new Field('distance', 'Double'))
	Schema schema = new Schema(inLayer.name, fields)
	List outFeatures = []

	inLayer.eachFeature { inFeature ->
		def geom = inFeature.getGeom()
		def distance = geom.distance(inPoint)
		def attrs = inFeature.getAttributes()
		attrs.put('distance', distance)
		Feature outFeature = new Feature(attrs, inFeature.getId())
		outFeatures.add(outFeature)
	}
	
	Layer result = new Layer(inLayer.name, schema)
	outFeatures.sort{it.get('distance')}
	outFeatures.each { outFeature ->
		result.add(outFeature)
	}
	
	[result: result]
}

